package com.github.gyunstorys.nlp.api.morph.controller;

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
                            morphResult.put("type", morph.getFeatureHead());
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
