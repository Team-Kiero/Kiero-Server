package com.kiero.invitation.util;

import com.kiero.invitation.enums.InviteCodeFirstWord;
import com.kiero.invitation.enums.InviteCodeSecondWord;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class InviteCodeGenerator {
    
    // 캐싱
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final InviteCodeFirstWord[] FIRST_WORDS = InviteCodeFirstWord.values();
    private static final InviteCodeSecondWord[] SECOND_WORDS = InviteCodeSecondWord.values();

    public String generate() {
        String word1 = FIRST_WORDS[RANDOM.nextInt(FIRST_WORDS.length)].getValue();
        String word2 = SECOND_WORDS[RANDOM.nextInt(SECOND_WORDS.length)].getValue();
        int number = RANDOM.nextInt(1000);

        return String.format("%s%s%03d", word1, word2, number);
    }
}