# nlp-api
## ETRI KoBERT에서 사용하기 위해 만든 ETRI 형태소 분석기를 대체 할 Mecab형태소 분석기 API

### 1. Usages
테스트는 우분투 및 osx에서 진행하였고 java로 코드를 작성하였기 때문에 OS와 관계없이 JDK만 설치되어 있으면 구동 

#### 1.1. Install
1. git pull https://github.com/Gyunstorys/nlp-api
2. Install JDK 1.8 version
    1. Ubuntu
        * sudo apt install software-properties-common
        * add-apt-repository ppa:webupd8team/java
        * apt-get update
        * apt-get install oracle-java8-installer
            
            

#### 1.2. run
* cd nlp-api
* src/main/resources/application.properties 수정 
    * dictionary.path => 절대경로로  nlp-api directory내에 있는 user.dic 경로 
* ./mvnw spring-boot:run <br/>
처음실행 시 필요한 dependency를 설치하는 과정때문에 시간이 조금 소요가 됩니다.

#### 1.3. API
Request
* GET Method
    * http://localhost:8080/api/morpheme/etri?targetText=안녕하세요. 한국어테스트 입니다.
    <br/>※ url encoding 필요.
    
* Post Method
    * url: http://localhost:8080/api/morpheme/etri
    * body: ?targetText=안녕하세요. 한국어테스트 입니다<br/> ※ url encoding 필요.

Response
<pre><code>
{
  "request_id": "reserved field",
  "result": 0,
  "return_type": "com.google.gson.internal.LinkedTreeMap",
  "return_object": "com.google.gson.internal.LinkedTreeMap",
  "sentences": [
    {
      "id": 0,
      "text": "안녕하세요. 한국어테스트 입니다.",
      "morp": [
        {
          "id": 0,
          "position": 0,
          "weight": 1708,
          "type": "NNG",
          "lemma": "안녕"
        },
        {
          "id": 1,
          "position": 6,
          "weight": 2460,
          "type": "XSV",
          "lemma": "하"
        },
        {
          "id": 2,
          "position": 9,
          "weight": 0,
          "type": "EP",
          "lemma": "시"
        }
        ...
      ]
    }
  ]
}
</code></pre>
### 2. mecab 품사 태그 > 세종 품사 태그 규칙
* konlpy에서 제공하는 품사 태그 비교표를 참조하여 작성하였습니다.</br>
https://docs.google.com/spreadsheets/d/1OGAjUvalBuX-oZvZ_-9tEfYD2gQe7hTGsgUpiiBSXI8/edit#gid=0
* 차이점 (왼쪽이 mecab태그 이고 우측이 세종태그 입니다.)
    * SSO	여는 괄호 (, [ 
        * SS 따옴표,괄호표,줄표
    * SSC	닫는 괄호 ), ] 
        * SS 따옴표,괄호표,줄표
    * SSC	닫는 괄호 ), ] 
        * SS 따옴표,괄호표,줄표
    * SC	구분자 , · / :
        * SP 쉼표,가운뎃점,콜론,빗금
    * SY    기타기호
        * SW 기타기호(논리수학기호,화폐기호)와 SO 붙임표(물결,숨김,빠짐) 두가지로 분리 하였습니다.
          	
<pre><code>
private static final Set<String> SW_POS = new HashSet<String>(){{
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
    private static final Set<String> SO_POS = new HashSet<String>(){{
        add("~");
        add("-");
    }};
    /**
     * Convert etri pos tag string.
     *
     * @param surface the surface
     * @param pos     the pos
     * @return the string
     */
    public static String convertEtriPosTag(String surface,String pos) {
        if (pos.equals("SF"))
            return pos;
        else if (pos.equals("SC"))
            return "SP";
        else if (pos.equals("NNBC"))
            return "NNB";
        else if (pos.equals("SSO") || pos.equals("SSC"))
            return "SS";
        else if (SW_POS.contains(surface))
            return "SW";
        else if (SO_POS.contains(surface))
            return "SO";
        else if (pos.equals("SY"))
            return "SW";
        else if (pos.equals("UN"))
            return "UNK";
        return pos;
    }
</code></pre>
### 3. 사용 한 라이브러리
* seunjeon(은전한닢)
    * https://bitbucket.org/eunjeon/seunjeon/src/master/


#### partner 
* https://github.com/domyounglee (@domyunglee)
