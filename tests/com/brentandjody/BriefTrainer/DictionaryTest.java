package com.brentandjody.BriefTrainer;

import org.junit.Test;

/**
 * Created by brentn on 17/02/14.
 * Determine which multi-stroke words that you use
 * can be replaced by shorter strokes
 */
public class DictionaryTest {
    @Test
    public void testLoad() throws Exception {
        Dictionary dictionary = new Dictionary();
        assert(dictionary.size() == 0);
        String[] filename = {"assets/dict.json"};
        dictionary.load(filename);
        assert(dictionary.size() > 0);
        int firstSize = dictionary.size();
        filename[0] = "assets/canadian.json";
        dictionary.load(filename);
        assert(dictionary.size() > firstSize);
        dictionary.unload();
        assert(dictionary.size() == 0);
    }

    @Test
    public void testLookup() throws Exception {
        Dictionary dictionary = new Dictionary();
        String[] filename = {"assets/dict.json"};
        assert(dictionary.lookup("color")==null);
        dictionary.load(filename);
        assert(dictionary.lookup("colour").size()==1);
        assert(dictionary.lookup("color").contains("KHROR"));
        filename[0] = "assets/canadian.json";
        dictionary.load(filename);
        assert(dictionary.lookup("colour").size()>1);
        assert(dictionary.lookup("colour").contains("KHROR"));
    }

    @Test
    public void testPossibilities() throws Exception {
        Dictionary dictionary = new Dictionary();
        String[] filename = {"assets/dict.json"};
        dictionary.load(filename);
        assert(dictionary.possibilities("oranging",100).size()==0);
        int size = dictionary.possibilities("spin",100).size();
        assert(size > 0);
        assert(dictionary.possibilities("spinner",100).size()<size);
    }

}
