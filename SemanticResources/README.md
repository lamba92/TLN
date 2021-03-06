## Section II - Semantic Resources

 - [Introduction](#introduction)
 - [Conceptual Similarity](#conceptual-similarity)
    - [code](/ConceptualSimilarity)
 - [Semantic Evaluation](#semantic-evaluation)
    - [code](/ConceptualSimilarity) 
 - [Word Sense Disambiguation](#word-sense-disambiguation)
    - [External Colab notebook](https://colab.research.google.com/drive/1TgXt3uMpd4FZG-PgVUpmX8BIU2VbmQ8c?usp=sharing)
 - [FrameNet](#framenet)
    - [External Colab notebook](https://colab.research.google.com/drive/197TvEHI6po328nwBvYzWOk3eS3jJBb--?usp=sharing)
 - [Automatic Summarization](#automatic-summarization)
    - [code](/AutomaticSummarization) 

### Introduction
This sections shows how to use some available Semantic Resources like FrameNet, WordNet, Nasari, BabelNet and SemCor.

Also, many semantic disambiguation algorithms where implemented, both in Kotlin and Python. Why Kotlin? Well, Python is from 1994, Kotlin is 3 years old... Whenever semantic resources where available for one of the backends of Kotlin (JS, JVM, Native) we went for Kotlin for faster development time! 

### Conceptual Similarity
Given 2 input terms, a conceptual similarity task involves finding a similarity score based of their semantic similarity.
More specifically a score of:
- 0 means total dissimilarity
- 1 means identity

We relied on WordNet to lookup all possible terms meanings.

#### Input 
the **input** for this exercise is a **file** named `WordSim353` in which we can identify **353 pairs of terms**.

Each pair has an attribute from **[0,10]** which scores the **semantic similarity** between the elements.

#### Objective
**Implement 3 different score measurement** based on WordNet, that is:

- **Wu & Palmer** 
   <p align="center">
        <img src="https://latex.codecogs.com/gif.latex?cs%28s1%2Cs2%29%3D%5Cfrac%7B2*depth%28LCS%29%7D%7Bdepth%28s1%29&plus;depth%28s2%29%7D">
   </p> where <i>LCS</i> is the lowest common ancestor (also called <i>Lowest Common Subsumer</i>) between the senses <i>s1</i> and <i>s2</i>, and <i>depth(x)</i> is a distance function between WordNet root and the synset <i>x</i>.

- **Shortest Path**
   <p align="center">
        <img src="https://latex.codecogs.com/gif.latex?sim_%7Bpath%7D%28s1%2Cs2%29%3D2*depthMax%20-%20len%28s1%2Cs2%29">
   </p> where <i>depthMax</i> is the max depth of WordNet.

- **Leakcock & Chodorow**
<p align="center">
     <img src="https://latex.codecogs.com/gif.latex?sim_%7BLC%7D%28s1%2Cs2%29%3D-log%5Cfrac%7Blen%28s1%2Cs2%29%7D%7B2*depthMax%7D">
</p>

<hr>    
    
- **Correlation indexes**

 - **Spearman**: 
 <p align="center">
 <img src="https://latex.codecogs.com/gif.latex?r_s%3D%5Crho_%7Brg_X%2Crg_Y%7D%3D%5Cfrac%7Bcov%28rg_X%2Crg_Y%29%7D%7B%5Csigma_%7Brg_X%7D%5Csigma_%7Brg_Y%7D%7D">
</p>

 - **Pearson**:
 <p align="center">
 <img src="https://latex.codecogs.com/gif.latex?%5Crho_%7BX%2CY%7D%3D%5Cfrac%7Bcov%28X%2CY%29%7D%7B%5Csigma_%7BX%7D%5Csigma_%7BY%7D%7D">
 </p>
     

### Semantic Evaluation

#### Input 
The **input** for this exercise are 2 **file**s called [`annotations/basti.txt`](SemanticEvaluation/src/main/resources/annotations/basti.txt) and [`annotations/pregno.txt`](SemanticEvaluation/src/main/resources/annotations/pregno.txt) in which 50 of all pairs of words have been annotated with a score from 0 to 4 upon the opinion of the annotator.

#### Objective

Using the previous exercise correlation indexes, score how each annotator differs from the cosine similarity of all NASARI meaning vectors.

Also, correlate how much the human annotators differs from each other.

#### Sense identification
Consists in evaluating for each possible meaning of 2 terms:
<p align="center">
 <img src="https://latex.codecogs.com/gif.latex?c_{1},&space;c_{2}&space;\longleftarrow&space;\underset{c_{1}&space;\in&space;s\left(w_{1}\right),&space;c_{2}&space;\in&space;s\left(w_{2}\right)}{\arg&space;\max&space;}\left[\operatorname{sim}\left(c_{1},&space;c_{2}\right)\right]" title="c_{1}, c_{2} \longleftarrow \underset{c_{1} \in s\left(w_{1}\right), c_{2} \in s\left(w_{2}\right)}{\arg \max }\left[\operatorname{sim}\left(c_{1}, c_{2}\right)\right]" />
</p> 

To do so **cosine similarity** between *NASARI vectors* has been used: 
  <p align="center">
  <img src="https://latex.codecogs.com/gif.latex?cos\_similarity(A,B)=\frac{A&space;\cdot&space;B}{\|A\|\|B\|}" title="cos\_similarity(A,B)=\frac{A \cdot B}{\|A\|\|B\|}" />
  </p>

### Word Sense Disambiguation

*"Word sense disambiguation (WSD) is an open problem of natural language processing, which comprises the process of identifying which sense of a word (i.e. meaning) is used in any given sentence, when the word has a number of distinct senses (polisemia)."*


#### Objective
1. Implement the **Lesk algorithm**
2. Extract 50 sentences from *SemCor* and disambiguate a noun per sentence. **Also evaluate the accuracy between the SemCor annotated senses, and the ones found using Lesk algorithm**

#### Lesk Algorithm
**Pseudocode**:
<p align="center">
 <img src="https://user-images.githubusercontent.com/37592014/60672048-c03b0880-9e74-11e9-8769-27215887fb7b.PNG" width="60%" height="30%">
</p>

Variants that have been implemented:
1. **the one shown above**. 
2. the one shown above but **with stopwords removed**. 
3. with **context extension**: adds to the context all *hypernims* and *hyponims* of all terms in the sentence.

Results:

|  | Lesk v1 | Lesk v2 | Lesk v3 |
| ------------- | ------------- | ------------- | ------------- |
| **Accuracy**         |  0.46  | 0.46  | 0.56 |  


### FrameNet

First operation is to find a set of Frames using `get_frame_set_for_student(surname)`. Afterward for each Frame in the set we assigned a WordNet Synset at each of:
- Frame name
- Frame Elements (FEs)
- Lexical Units (LUs)

Now, using the **bag of words** algorithm, a contex was created from every synset looked up from lemmas of the previous list and compared with the context of the Frame.
The Frame context has been built getting every word and stemming them from:
 - Frame Name
 - definitions
 - for each LE:
   - name
   - definition
   - examples
 - Frame Elements names
 
The synset context has been build getting every word and stemming them from:
 - definition
 - examples
 - lemma names

Bag of words formula:
![proj image](assets/bag_of_word.png)

After selecting the synset with the highest score for each item of the frame, everything is printed out, for each frame for both students:

Student: **Basti**

|  ID | frame |
| ------------- | ------------- |
| 810 | Measure_by_action | 
| 347 | Revenge | 
| 1041 | Left_to_do | 
| 1182 | Post_receiving | 
| 1322 | Active_substance | 

Student: **Pregno**

|  ID | frame |
| ------------- | ------------- |
| 5 | Causation | 
| 384 | Experience_bodily_harm | 
| 228 | Being_named | 
| 2654 | Controller_object | 
| 156 | Measure_area | 

### Automatic Summarization

The purpose of this exercise is to produce brief of a text including the more relevant information.
The text to summarize for this task are in the resource/corpus directory.

In order to accomplish the goal we procede splitting every text in two parts:
1. title
2. paragraphs

on both parts we perform a "cleaning" removing the stopwords and tokenizing.

In order to extract the most significant paragraph, 
we calculate the similarity between the title and every paragraph taking the one with the highest score.

More in detail:
- for every word of title: we take the Nasari ids (trough the corresponding Bablesynset id)
    - for every paragraph
        - for every word of this paragraph: we do the same of title and take the Nasari ids
        we compute the Weighted Overlap between the title and the paragraph word as follows:
        
            ![WO](assets/weightedOverlap.png)
        
        - then we take the max of the square root of the WO
        
            ![MS](assets/maxSimilarity.png)
        
Now we have the similarity between every word in the title and in the paragraphs.
Simply sum up every score of the words in each paragraph taking the one with the maximum sum.
