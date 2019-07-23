package com.github.gyunstorys.nlp.api.morph.controller;

import com.codepoetics.protonpack.StreamUtils;
import com.github.gyunstorys.nlp.api.morph.vo.ResponseVo;
import org.bitbucket.eunjeon.seunjeon.Analyzer;
import org.bitbucket.eunjeon.seunjeon.LNode;
import org.bitbucket.eunjeon.seunjeon.Morpheme;
import org.openkoreantext.processor.OpenKoreanTextProcessorJava;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scala.Tuple2;
import scala.collection.JavaConverters;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The type Morph controller.
 */
@RestController
@RequestMapping(value = "/api/morpheme")
public class MorphController {
    @Value("${dictionary.path}")
    private String path;

    @PostConstruct
    @RequestMapping(value = "/dictionary")
    public ResponseVo loadUserDictionary(){
        System.out.println("경로 : " + path);
        ResponseVo responseVo = new ResponseVo();
        try(BufferedReader br =new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))))){
            String temp = null;
            Set<String> dictionary = new LinkedHashSet<>();
            while ((temp=br.readLine())!=null){
                temp = temp.trim();
                if (StringUtils.isEmpty(temp) || temp.startsWith("#"))
                    continue;
                String[] arr = temp.split("\\s");
                for (String noun : arr){
                    System.out.println(noun);
                    dictionary.add(noun);
                }
                responseVo.setData(dictionary);
                responseVo.setMessage("사전 로드 성공");
            }
            Analyzer.setUserDict(dictionary.iterator());
        }catch (Exception e){
            e.printStackTrace();
            responseVo.setMessage("사전 로드 실패");
            responseVo.setCode(1);
        }
        System.out.println(responseVo);
        return responseVo;

    }
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
        try {
            List result =
                    StreamSupport.stream(Analyzer.parseJava(targetText).spliterator(), false)
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
                            ).map(lnode -> {
                        List<Map<String, Object>> data = new ArrayList<>();
                        for (Morpheme morph : lnode._2) {
                            Map<String, Object> morphResult = new LinkedHashMap<>();
                            morphResult.put("id", atomicInteger.getAndIncrement());
                            morphResult.put("position", targetText.substring(0, lnode._1.beginOffset()).getBytes().length);
                            morphResult.put("weight", morph.getCost());
                            morphResult.put("type", convertEtriPosTag(morph.getSurface(),morph.getFeatureHead()));
                            morphResult.put("lemma", morph.getSurface());
                            data.add(morphResult);
                        }
                        return data;
                    }).flatMap(e -> e.stream()).collect(Collectors.toList());

            response.put("sentences",new ArrayList(){{
                add(new LinkedHashMap<String,Object>(){{
                    put("id",0);
                    put("text",targetText);
                    put("morp",result);
                }});
            }});
            return response;
        }catch (Exception e){
            System.out.println(targetText);
            e.printStackTrace();
        }
        response.put("result",1);
        return response;

    }

    @RequestMapping(value = "/etri/sentences")
    public Map<String,Object> getMecabToEtriFormatSentence(String targetText){
        AtomicInteger atomicInteger = new AtomicInteger();
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("request_id","reserved field");
        response.put("result",0);
        response.put("return_type","com.google.gson.internal.LinkedTreeMap");
        response.put("return_object","com.google.gson.internal.LinkedTreeMap");
        try {
            List<Map<String,Object>> result = StreamUtils.zipWithIndex(OpenKoreanTextProcessorJava.splitSentences(targetText).stream())
                    .map(sentence->
                    {
                        int start = sentence.getValue().start();
                        int end = sentence.getValue().end();
                        String sentenceStr = targetText.substring(start,end);
                        List<LNode> sentenceNodes= StreamSupport.stream(Analyzer.parseJava(sentenceStr,true).spliterator(),false)
                                .collect(Collectors.toList());
                        Map<String, Object> resultSentence = new LinkedHashMap<>();
                        resultSentence.put("id",sentence.getIndex());
                        resultSentence.put("text",targetText.substring(start,end));
                        resultSentence.put("morph",sentenceNodes.stream()
                                .map(e -> e.deCompoundJava())
                                .flatMap(e -> e.stream())
                                .map(e -> {
                                            List<Morpheme> morphemes = null;
                                            if (e.morpheme().getPoses().size()==1)
                                                morphemes = new ArrayList<Morpheme>() {{
                                                    add(e.morpheme());
                                                }};
                                            else
                                                morphemes = JavaConverters.seqAsJavaList(e.morpheme().deComposite());
                                            return new Tuple2<>(e, morphemes);
                                        }
                                )
                                .map(lnode -> {
                                    List<Map<String, Object>> morphemes = new ArrayList<>();
                                    for (Morpheme morph : lnode._2) {
                                        Map<String, Object> morphResult = new LinkedHashMap<>();
                                        morphResult.put("id", atomicInteger.getAndIncrement());
                                        morphResult.put("position", targetText.substring(0, start+lnode._1.beginOffset()).getBytes().length);
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
        else if (pos.equals("UN"))
            return "UNK";
        return pos;
    }
}
