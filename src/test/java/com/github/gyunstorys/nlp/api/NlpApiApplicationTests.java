package com.github.gyunstorys.nlp.api;

import com.github.gyunstorys.nlp.api.morph.controller.MorphController;
import org.bitbucket.eunjeon.seunjeon.Analyzer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NlpApiApplicationTests {
    @Autowired
    private MorphController controller;
    @Test
    public void contextLoads() {
    }
    @Test
    public void morhTest(){

        System.out.println(controller.getMecabToEtriFormat("머리를 뽑아내는것은"));
    }

}
