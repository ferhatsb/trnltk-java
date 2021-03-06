/*
 * Copyright  2013  Ali Ok (aliokATapacheDOTorg)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.trnltk.morphology.lexicon;

import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.trnltk.model.lexicon.ImmutableLexeme;
import org.trnltk.model.lexicon.ImmutableRoot;
import org.trnltk.model.lexicon.Lexeme;
import org.trnltk.model.lexicon.LexemeAttribute;
import org.trnltk.model.lexicon.PrimaryPos;
import org.trnltk.model.lexicon.PhoneticAttribute;
import org.trnltk.model.lexicon.PhoneticExpectation;
import org.trnltk.model.letter.TurkicLetter;
import org.trnltk.model.letter.TurkishAlphabet;
import org.trnltk.morphology.phonetics.PhoneticsAnalyzer;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class ImmutableRootGenerator {
    private static final ImmutableSet<LexemeAttribute> MODIFIERS_TO_WATCH = Sets.immutableEnumSet(LexemeAttribute.Doubling,
            LexemeAttribute.LastVowelDrop,
            LexemeAttribute.ProgressiveVowelDrop,
            LexemeAttribute.InverseHarmony,
            LexemeAttribute.Voicing,
            LexemeAttribute.VoicingOpt,
            LexemeAttribute.Special,
            LexemeAttribute.EndsWithAyn);

    private static final ImmutableMap<Pair<String, PrimaryPos>, String> rootChanges = new ImmutableMap.Builder<Pair<String, PrimaryPos>, String>()
            .put(Pair.of("ben", PrimaryPos.Pronoun), "ban")
            .put(Pair.of("sen", PrimaryPos.Pronoun), "san")
            .put(Pair.of("demek", PrimaryPos.Verb), "di")
            .put(Pair.of("yemek", PrimaryPos.Verb), "yi")
            .put(Pair.of("hepsi", PrimaryPos.Pronoun), "hep")
            .put(Pair.of("ora", PrimaryPos.Pronoun), "or")
            .put(Pair.of("bura", PrimaryPos.Pronoun), "bur")
            .put(Pair.of("şura", PrimaryPos.Pronoun), "şur")
            .put(Pair.of("nere", PrimaryPos.Pronoun), "ner")
            .put(Pair.of("içeri", (PrimaryPos) null), "içer") // applicable to all forms of the word
            .put(Pair.of("dışarı", (PrimaryPos) null), "dışar") // applicable to all forms of the word
            .put(Pair.of("birbiri", PrimaryPos.Pronoun), "birbir")
            .build();

    private PhoneticsAnalyzer phoneticsAnalyzer = new PhoneticsAnalyzer();

    public Collection<ImmutableRoot> generateAll(final Set<Lexeme> lexemes) {
        HashSet<ImmutableRoot> all = new HashSet<ImmutableRoot>();
        for (Lexeme lexeme : lexemes) {
            all.addAll(this.generate(lexeme));
        }
        return all;
    }

    public HashSet<ImmutableRoot> generate(final Lexeme lexeme) {
        if (CollectionUtils.containsAny(lexeme.getAttributes(), MODIFIERS_TO_WATCH)) {
            return this.generateModifiedRootNodes(lexeme);
        } else {
            Set<PhoneticAttribute> phoneticAttributes = phoneticsAnalyzer.calculatePhoneticAttributes(lexeme.getLemmaRoot(), lexeme.getAttributes());
            final ImmutableRoot root = new ImmutableRoot(lexeme.getLemmaRoot(), lexeme, Sets.immutableEnumSet(phoneticAttributes), null);
            return Sets.newHashSet(root);
        }
    }

    private HashSet<ImmutableRoot> generateModifiedRootNodes(final Lexeme lexeme) {
        final Set<LexemeAttribute> lexemeAttributes = lexeme.getAttributes();
        if (lexemeAttributes.contains(LexemeAttribute.Special))
            return this.handleSpecialRoots(lexeme);

        if (lexemeAttributes.contains(LexemeAttribute.EndsWithAyn)) {       //kind of hack, didn't like it :(
            // if the word ends with Ayn
            // create roots with that attribute, and without that attribute
            // when creating with that attribute, add a VowelStart expectation

            final HashSet<ImmutableRoot> immutableRoots = new HashSet<ImmutableRoot>();

            final EnumSet<LexemeAttribute> lexemeAttributesWithoutAyn = EnumSet.copyOf(lexemeAttributes);
            lexemeAttributesWithoutAyn.remove(LexemeAttribute.EndsWithAyn);
            final Lexeme lexemeWithoutAttrEndsWithAyn = new ImmutableLexeme(lexeme.getLemma(), lexeme.getLemmaRoot(), lexeme.getPrimaryPos(), lexeme.getSecondaryPos(), Sets.immutableEnumSet(lexemeAttributesWithoutAyn));
            final HashSet<ImmutableRoot> rootsWithoutAynApplied = this.generateModifiedRootNodes(lexemeWithoutAttrEndsWithAyn);
            immutableRoots.addAll(rootsWithoutAynApplied);

            for (ImmutableRoot immutableRoot : rootsWithoutAynApplied) {
                final ImmutableSet<PhoneticAttribute> phoneticAttributesWithoutAynApplied = immutableRoot.getPhoneticAttributes();
                final HashSet<PhoneticAttribute> phoneticAttributesWithAynApplied = Sets.newHashSet(phoneticAttributesWithoutAynApplied);
                phoneticAttributesWithAynApplied.remove(PhoneticAttribute.LastLetterVowel);
                phoneticAttributesWithAynApplied.add(PhoneticAttribute.LastLetterConsonant);
                final ImmutableRoot immutableRootWithAynApplied = new ImmutableRoot(immutableRoot.getSequence(), immutableRoot.getLexeme(), Sets.immutableEnumSet(phoneticAttributesWithAynApplied), Sets.immutableEnumSet(PhoneticExpectation.VowelStart));
                immutableRoots.add(immutableRootWithAynApplied);
            }

            // just before returning, set correct lexeme again
            final HashSet<ImmutableRoot> immutableRootsWithCorrectLexemeAttr = new HashSet<ImmutableRoot>();
            for (ImmutableRoot immutableRoot : immutableRoots) {
                immutableRootsWithCorrectLexemeAttr.add(new ImmutableRoot(immutableRoot.getSequence(), lexeme, immutableRoot.getPhoneticAttributes(), immutableRoot.getPhoneticExpectations()));
            }

            return immutableRootsWithCorrectLexemeAttr;
        }

        final String lemmaRoot = lexeme.getLemmaRoot();
        String modifiedRootStr = lexeme.getLemmaRoot();

        final EnumSet<PhoneticAttribute> originalPhoneticAttrs = phoneticsAnalyzer.calculatePhoneticAttributes(lexeme.getLemmaRoot(), null);
        final EnumSet<PhoneticAttribute> modifiedPhoneticAttrs = phoneticsAnalyzer.calculatePhoneticAttributes(lexeme.getLemmaRoot(), null);

        final EnumSet<PhoneticExpectation> originalPhoneticExpectations = EnumSet.noneOf(PhoneticExpectation.class);
        final EnumSet<PhoneticExpectation> modifiedPhoneticExpectations = EnumSet.noneOf(PhoneticExpectation.class);

        if (CollectionUtils.containsAny(lexemeAttributes, Sets.immutableEnumSet(LexemeAttribute.Voicing, LexemeAttribute.VoicingOpt))) {
            final TurkicLetter lastLetter = TurkishAlphabet.getLetter(modifiedRootStr.charAt(modifiedRootStr.length() - 1));
            final TurkicLetter voicedLastLetter = lemmaRoot.endsWith("nk") ? TurkishAlphabet.L_g : TurkishAlphabet.voice(lastLetter);
            Validate.notNull(voicedLastLetter);
            modifiedRootStr = modifiedRootStr.substring(0, modifiedRootStr.length() - 1) + voicedLastLetter.charValue();

            modifiedPhoneticAttrs.remove(PhoneticAttribute.LastLetterVoicelessStop);

            if (!lexemeAttributes.contains(LexemeAttribute.VoicingOpt)) {
                originalPhoneticExpectations.add(PhoneticExpectation.ConsonantStart);
            }

            modifiedPhoneticExpectations.add(PhoneticExpectation.VowelStart);
        }

        if (lexemeAttributes.contains(LexemeAttribute.Doubling)) {
            modifiedRootStr += modifiedRootStr.charAt(modifiedRootStr.length() - 1);
            originalPhoneticExpectations.add(PhoneticExpectation.ConsonantStart);
            modifiedPhoneticExpectations.add(PhoneticExpectation.VowelStart);
        }

        if (lexemeAttributes.contains(LexemeAttribute.LastVowelDrop)) {
            modifiedRootStr = modifiedRootStr.substring(0, modifiedRootStr.length() - 2) + modifiedRootStr.charAt(modifiedRootStr.length() - 1);
            if (!PrimaryPos.Verb.equals(lexeme.getPrimaryPos()))
                originalPhoneticExpectations.add(PhoneticExpectation.ConsonantStart);

            modifiedPhoneticExpectations.add(PhoneticExpectation.VowelStart);
        }

        if (lexemeAttributes.contains(LexemeAttribute.InverseHarmony)) {
            originalPhoneticAttrs.add(PhoneticAttribute.LastVowelFrontal);
            originalPhoneticAttrs.remove(PhoneticAttribute.LastVowelBack);
            modifiedPhoneticAttrs.add(PhoneticAttribute.LastVowelFrontal);
            modifiedPhoneticAttrs.remove(PhoneticAttribute.LastVowelBack);
        }

        if (lexemeAttributes.contains(LexemeAttribute.ProgressiveVowelDrop)) {
            modifiedRootStr = modifiedRootStr.substring(0, modifiedRootStr.length() - 1);
            if (this.hasVowel(modifiedRootStr)) {
                modifiedPhoneticAttrs.clear();
                modifiedPhoneticAttrs.addAll(phoneticsAnalyzer.calculatePhoneticAttributes(modifiedRootStr, null));
            }
            modifiedPhoneticExpectations.add(PhoneticExpectation.VowelStart);
        }

        ImmutableRoot originalRoot = new ImmutableRoot(lexeme.getLemmaRoot(), lexeme, Sets.immutableEnumSet(originalPhoneticAttrs), Sets.immutableEnumSet(originalPhoneticExpectations));
        ImmutableRoot modifiedRoot = new ImmutableRoot(modifiedRootStr, lexeme, Sets.immutableEnumSet(modifiedPhoneticAttrs), Sets.immutableEnumSet(modifiedPhoneticExpectations));

        if (originalRoot.equals(modifiedRoot))
            return Sets.newHashSet(originalRoot);
        else
            return Sets.newHashSet(originalRoot, modifiedRoot);
    }

    private HashSet<ImmutableRoot> handleSpecialRoots(final Lexeme originalLexeme) {
        String changedRootStr = rootChanges.get(Pair.of(originalLexeme.getLemma(), originalLexeme.getPrimaryPos()));
        if (StringUtils.isBlank(changedRootStr))
            changedRootStr = rootChanges.get(Pair.of(originalLexeme.getLemma(), (PrimaryPos) null));

        Validate.notNull(changedRootStr, "Unhandled root change : " + originalLexeme);

        final Set<LexemeAttribute> attributes = originalLexeme.getAttributes();
        final EnumSet<LexemeAttribute> newAttributes = EnumSet.copyOf(attributes);
        newAttributes.remove(LexemeAttribute.Special);
        final Lexeme modifiedLexeme = new ImmutableLexeme(originalLexeme.getLemma(), originalLexeme.getLemmaRoot(), originalLexeme.getPrimaryPos(),
                originalLexeme.getSecondaryPos(), Sets.immutableEnumSet(newAttributes));


        final String unchangedRootStr = originalLexeme.getLemmaRoot();

        final ImmutableRoot rootUnchanged = new ImmutableRoot(unchangedRootStr, modifiedLexeme,
                Sets.immutableEnumSet(phoneticsAnalyzer.calculatePhoneticAttributes(unchangedRootStr, modifiedLexeme.getAttributes())), null);

        final ImmutableRoot rootChanged = new ImmutableRoot(changedRootStr, modifiedLexeme,
                Sets.immutableEnumSet(phoneticsAnalyzer.calculatePhoneticAttributes(changedRootStr, modifiedLexeme.getAttributes())), null);

        return Sets.newHashSet(rootUnchanged, rootChanged);

    }

    private boolean hasVowel(String str) {
        return Iterables.any(Lists.newArrayList(ArrayUtils.toObject(str.toCharArray())), new Predicate<Character>() {
            @Override
            public boolean apply(Character input) {
                return TurkishAlphabet.getLetter(input).isVowel();
            }
        });
    }
}