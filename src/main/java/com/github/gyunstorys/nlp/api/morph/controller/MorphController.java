package com.github.gyunstorys.nlp.api.morph.controller;

import one.util.streamex.EntryStream;
import org.bitbucket.eunjeon.seunjeon.Analyzer;
import org.bitbucket.eunjeon.seunjeon.LNode;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The type Morph controller.
 */
@RestController
@RequestMapping(value = "/api/morpheme")
public class MorphController {

    /**
     * Get mecab to etri format map.
     *
     * @param targetText the target text
     * @return the map
     */
    @RequestMapping(value = "/etri")
    public Map<String,Object> getMecabToEtriFormat(String targetText){
        List result =EntryStream.of(
                StreamSupport.stream(Analyzer.parseJava(targetText).spliterator(),false)
                        .map(LNode::deInflectJava)
                        .flatMap(e-> e.stream())
                        .collect(Collectors.toList())
        ).map(entry -> {
            int key = entry.getKey();
            LNode morph = entry.getValue();
            Map<String, Object> morphResult = new LinkedHashMap<>();
            morphResult.put("id", key);
//            morphResult.put("position", morph.beginOffset());
            morphResult.put("position", targetText.substring(0,morph.beginOffset()).getBytes().length);
            morphResult.put("weight", morph.accumulatedCost());
            morphResult.put("type", convertEtriPosTag(morph));
            morphResult.put("lemma", morph.morpheme().getSurface());
            return morphResult;
        }).collect(Collectors.toList());
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("request_id","reserved field");
        response.put("result",0);
        response.put("return_type","com.google.gson.internal.LinkedTreeMap");
        response.put("return_object","com.google.gson.internal.LinkedTreeMap");
        response.put("sentences",new ArrayList(){{
            add(new LinkedHashMap<String,Object>(){{
                put("id",0);
                put("text",targetText);
                put("morp",result);
            }});
        }});
        return response;
    }

    private String convertEtriPosTag(LNode morph) {
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
