package com.github.gyunstorys.nlp.api.nlpapi.morph.utils;

import one.util.streamex.EntryStream;
import org.bitbucket.eunjeon.seunjeon.Analyzer;
import org.bitbucket.eunjeon.seunjeon.LNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Mecab {
    public static List<Map<String, Object>> getMecabMorph(String text){
        return EntryStream.of(
                StreamSupport.stream(Analyzer.parseJava(text).spliterator(),false)
                        .map(LNode::deInflectJava)
                        .flatMap(e-> e.stream())
                        .collect(Collectors.toList())
        ).map(entry -> {
            int key = entry.getKey();
            LNode morph = entry.getValue();
            Map<String, Object> morphResult = new LinkedHashMap<>();
            morphResult.put("id", key);
//            morphResult.put("position", morph.beginOffset());
            morphResult.put("position", text.substring(0,morph.beginOffset()).getBytes().length);
            morphResult.put("weight", morph.accumulatedCost());
            morphResult.put("type", convertEtriPosTag(morph));
            morphResult.put("lemma", morph.morpheme().getSurface());
            return morphResult;
        }).collect(Collectors.toList());
    }
    private static String convertEtriPosTag(LNode morph) {
        String pos =  morph.morpheme().getFeatureHead();
        List<String> swPOS = new ArrayList<String>(){{
            add("@");
            add("#");
            add("$");
            add("%");
            add("^");
            add("&");
            add("*");
            add("_");
            add("+");
            add("=");
            add("`");
        }};
        List<String> soPOS = new ArrayList<String>(){{
            add("~");
            add("-");
        }};

        if (pos.equals("SF"))
            return pos;
        else if (pos.equals("SC"))
            return "SP";
        else if (pos.equals("NNBC"))
            return "NNB";
        else if (pos.equals("SSO") || pos.equals("SSC"))
            return "SS";
        else if (swPOS.contains(morph.morpheme().getSurface()))
            return "SW";
        else if (soPOS.contains(morph.morpheme().getSurface()))
            return "SO";
        else if (pos.equals("SY"))
            return "SW";
        return pos;
    }
}
