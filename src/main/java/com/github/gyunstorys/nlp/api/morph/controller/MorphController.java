package com.github.gyunstorys.nlp.api.morph.controller;

import com.codepoetics.protonpack.StreamUtils;
import kr.bydelta.koala.hnn.SentenceSplitter;
import one.util.streamex.EntryStream;
import org.bitbucket.eunjeon.seunjeon.Analyzer;
import org.bitbucket.eunjeon.seunjeon.LNode;
import org.bitbucket.eunjeon.seunjeon.Morpheme;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scala.Tuple2;
import scala.collection.JavaConverters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
        AtomicInteger atomicInteger = new AtomicInteger();
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("request_id","reserved field");
        response.put("result",0);
        response.put("return_type","com.google.gson.internal.LinkedTreeMap");
        response.put("return_object","com.google.gson.internal.LinkedTreeMap");
        SentenceSplitter splitter = new SentenceSplitter();
        try {
            AtomicInteger preSentenceLength = new AtomicInteger();
            List<Map<String,Object>> result = StreamUtils.zipWithIndex(splitter.jSentences(targetText).stream())
                    .map(sentence->
                    {
                        Map<String, Object> resultSentence = new LinkedHashMap<>();
                        preSentenceLength.addAndGet(sentence.getValue().getBytes().length);
                        resultSentence.put("id",sentence.getIndex());
                        resultSentence.put("text",sentence.getValue());
                        resultSentence.put("morph",StreamSupport.stream(Analyzer.parseJava(sentence.getValue()).spliterator(), false)
                                .map(e -> e.deCompoundJava())
                                .flatMap(e -> e.stream())
                                .map(e -> {
                                            List<Morpheme> morphemes = JavaConverters.seqAsJavaList(e.morpheme().deComposite());
                                            if (morphemes.size() == 0)
                                                morphemes = new ArrayList<Morpheme>() {{
                                                    add(e.morpheme());
                                                }};
                                            return new Tuple2<>(e, morphemes);
                                        }
                                )
                                .map(lnode -> {
                                    List<Map<String, Object>> morphemes = new ArrayList<>();
                                    for (Morpheme morph : lnode._2) {
                                        Map<String, Object> morphResult = new LinkedHashMap<>();
                                        morphResult.put("id", atomicInteger.getAndIncrement());
                                        morphResult.put("position", preSentenceLength.get()+sentence.getValue().substring(0, lnode._1.beginOffset()).getBytes().length);
                                        morphResult.put("weight", morph.getCost());
                                        morphResult.put("type", convertEtriPosTag(morph.getSurface(), morph.getFeatureHead()));
                                        morphResult.put("lemma", morph.getSurface());
                                        morphemes.add(morphResult);
                                    }
                                    return morphemes;
                                }).flatMap(e->e.stream())
                                .collect(Collectors.toList()));
                        return resultSentence;
                    }).collect(Collectors.toList());
            response.put("sentences",result);
            return response;
        }catch (Exception e){
            System.out.println(targetText);
            e.printStackTrace();
        }
        response.put("result",1);
        return response;

    }

    public static String convertEtriPosTag(String surface,String pos) {
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
        else if (swPOS.contains(surface))
            return "SW";
        else if (soPOS.contains(surface))
            return "SO";
        else if (pos.equals("SY"))
            return "SW";
        return pos;
    }
}
