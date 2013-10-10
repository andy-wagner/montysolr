package org.apache.lucene.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import monty.solr.util.MontySolrAbstractLuceneTestCase;
import monty.solr.util.MontySolrSetup;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MockIndexWriter;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.SecondOrderCollector.FinalValueType;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.LuceneTestCase.SuppressCodecs;
import org.apache.solr.schema.DateField;
import org.apache.solr.schema.FieldProperties;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TrieDateField;
//import org.apache.lucene.util.LuceneTestCase.BadApple;
import org.junit.BeforeClass;

//@BadApple
@SuppressCodecs("SimpleText")
public class TestSecondOrderQueryTypesAds extends MontySolrAbstractLuceneTestCase {

  protected String idField;
  private Directory directory;
  private IndexReader reader;
  private IndexSearcher searcher;
  private MockIndexWriter writer;

  

  @Override
  public void setUp() throws Exception {
    super.setUp();
    directory = newDirectory();
    addDocs(new float[]{0.1f,0.1f,0.1f,0.1f,0.1f,0.1f,0.1f,0.1f,0.1f,0.1f,});
  }

  private void addDocs(float[] b) throws IOException {
    assert b.length == 10;
    reOpenWriter(OpenMode.CREATE);

    int i=0;
    adoc("id", "0", "bibcode",   "b0", 
    		"const_boost", "1.0f", "boost", "0.5f", "ads_boost", "0.1f",
    		"date", "1966-01-01T00:00:00Z");
    adoc("id", "1", "bibcode",   "b1", 
    		"const_boost", "1.0f", "boost", "0.5f", "references", "b2,b3,b4,b5", "ads_boost", "0.1f",
    		"date", "1966-01-02T00:00:00Z");
    adoc("id", "2", "bibcode",   "b2", 
    		"const_boost", "1.0f", "boost", "0.2f", "ads_boost", "0.1f",
    		"date", "1966-01-03T00:00:00Z");
    adoc("id", "3", "bibcode",   "b3", 
    		"const_boost", "1.0f", "boost", "0.3f", "references", "b9", "ads_boost", "0.9f",
    		"date", "1966-01-03T01:00:00Z");
    adoc("id", "4", "bibcode",   "b4", 
    		"const_boost", "1.0f", "boost", "0.1f", "references", "b100", "ads_boost", "0.1f",
    		"date", "1966-01-03T01:01:00Z");
    adoc("id", "5", "bibcode",   "b5", 
    		"const_boost", "1.0f", "boost", "0.8f", "references", "b10", "ads_boost", "0.0f",
    		"date", "1966-01-03T01:01:01Z");
    
    writer.commit();
		reOpenWriter(OpenMode.APPEND); // close the writer, create a new segment
		
    adoc("id", "6", "bibcode",   "b6", "const_boost", "1.0f", "boost", "0.1f", "references", "b5", "ads_boost", "0.9f");
    adoc("id", "7", "bibcode",   "b7", "const_boost", "1.0f", "boost", "0.1f", "references", "b5", "ads_boost", "0.9f");
    adoc("id", "8", "bibcode",   "b8", "const_boost", "1.0f", "boost", "0.1f", "references", "b5", "ads_boost", "0.9f");
    adoc("id", "9", "bibcode",   "b9", "const_boost", "1.0f", "boost", "0.1f", "references", "b2,b3,b4,b10", "ads_boost", "0.9f");
    adoc("id", "10","bibcode",  "b10", "const_boost", "1.0f", "boost", "0.5f", "references", "b3,b4", "ads_boost", "0.1f");


    writer.commit();
		reOpenWriter(OpenMode.APPEND); // close the writer, create a new segment
		
		
    adoc("kw", "x", "ka", "b", "id", "16", "bibcode", "b16", "references", "b17,b18,b20", "ads_boost", "0.9f");       // links: 1
    adoc("kw", "x", "ka", "a", "id", "17", "bibcode", "b17", "references", "b16,b18,b20", "ads_boost", "0.7f");       // links: 2
    adoc("kw", "x", "ka", "b", "id", "18", "bibcode", "b18", "references", "b20", "ads_boost", "0.5f");               // links: 3
    adoc("kw", "x", "ka", "b", "id", "19", "bibcode", "b19", "references", "b17,b18,b20", "ads_boost", "0.3f");   // links: 0
    adoc("kw", "x", "ka", "b", "id", "20", "bibcode", "b20", "references", "b20", "ads_boost", "0.1f");               // links: 5
    

		
    reader = writer.getReader();
    searcher = newSearcher(reader);
    writer.close();
  }
  
  private void reOpenWriter(OpenMode mode) throws CorruptIndexException, LockObtainFailedException, IOException {
		if (writer != null) writer.close();
		writer = new MockIndexWriter(directory, newIndexWriterConfig(TEST_VERSION_CURRENT, 
				new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).setOpenMode(mode)
				//.setRAMBufferSizeMB(0.1f)
				//.setMaxBufferedDocs(500)
				);
	}

  @Override
  public void tearDown() throws Exception {
    reader.close();
    directory.close();
    super.tearDown();
  }

  private void adoc(String... fields) throws IOException {
    Document doc = new Document();
    DateField df = new TrieDateField();
    SchemaField sf = new SchemaField("test", new TrieDateField());
    
    for (int i = 0; i < fields.length; i = i + 2) {
      String f = fields[i];
      if (f.contains("boost")) {
        doc.add(new FloatField(f, Float.parseFloat(fields[i + 1]), Field.Store.YES));
      }
      //else if (f.contains("date")) {
      //	doc.add(df.createField(sf, fields[i+1], 1.0f));
      //}
      else {
        for (String v: fields[i + 1].split(",")) {
          doc.add(newField(fields[i], v, StringField.TYPE_STORED));
        }
      }
    }
    for (String v: "words are all the same".split(" ")) {
      doc.add(newField("text", v, StringField.TYPE_STORED));
      doc.add(newField("title", v, StringField.TYPE_STORED));
    }
    writer.addDocument(doc);
  }


  public void testADSOperators() throws Exception {


    String refField = "references";
    String[] idField = new String[] {"id"};


    // for the queries that use the String values
    // ------------------------------------------

    refField = "references";
    idField = new String[] {"bibcode"};
    int idPrefix = 0;

    Query d1 = new TermQuery(new Term("id", String.valueOf(idPrefix + 1)));
    Query d2 = new TermQuery(new Term("id", String.valueOf(idPrefix + 2)));
    Query d3 = new TermQuery(new Term("id", String.valueOf(idPrefix + 3)));
    Query d4 = new TermQuery(new Term("id", String.valueOf(idPrefix + 4)));
    Query d5 = new TermQuery(new Term("id", String.valueOf(idPrefix + 5)));
    Query d6 = new TermQuery(new Term("id", String.valueOf(idPrefix + 6)));
    Query d7 = new TermQuery(new Term("id", String.valueOf(idPrefix + 7)));
    Query d8 = new TermQuery(new Term("id", String.valueOf(idPrefix + 8)));
    Query d9 = new TermQuery(new Term("id", String.valueOf(idPrefix + 9)));
    Query d10 = new TermQuery(new Term("id", String.valueOf(idPrefix + 10)));
    Query d99 = new TermQuery(new Term("id", String.valueOf(idPrefix + 99)));

    Query r1 = new TermQuery(new Term("references", "b" + String.valueOf(idPrefix + 1)));
    Query r2 = new TermQuery(new Term("references", "b" + String.valueOf(idPrefix + 2)));
    Query r3 = new TermQuery(new Term("references", "b" + String.valueOf(idPrefix + 3)));
    Query r4 = new TermQuery(new Term("references", "b" + String.valueOf(idPrefix + 4)));
    Query r5 = new TermQuery(new Term("references", "b" + String.valueOf(idPrefix + 5)));
    Query r6 = new TermQuery(new Term("references", "b" + String.valueOf(idPrefix + 6)));
    Query r7 = new TermQuery(new Term("references", "b" + String.valueOf(idPrefix + 7)));
    Query r8 = new TermQuery(new Term("references", "b" + String.valueOf(idPrefix + 8)));
    Query r9 = new TermQuery(new Term("references", "b" + String.valueOf(idPrefix + 9)));
    Query r10 = new TermQuery(new Term("references", "b" + String.valueOf(idPrefix + 10)));
    Query r99 = new TermQuery(new Term("references", "b" + String.valueOf(idPrefix + 99)));


    BooleanQuery bq234 = new BooleanQuery();
    bq234.add(d2, Occur.SHOULD);
    bq234.add(d3, Occur.SHOULD);
    bq234.add(d4, Occur.SHOULD);

    BooleanQuery bq19 = new BooleanQuery();
    bq19.add(d1, Occur.SHOULD);
    bq19.add(d9, Occur.SHOULD);

    BooleanQuery bq1910 = new BooleanQuery();
    bq1910.add(d1, Occur.SHOULD);
    bq1910.add(d9, Occur.SHOULD);
    bq1910.add(d10, Occur.SHOULD);

    BooleanQuery bq26 = new BooleanQuery();
    bq26.add(d2, Occur.SHOULD);
    bq26.add(d6, Occur.SHOULD);

    Query all = new MatchAllDocsQuery();

    // just a test that index is OK
    assertEquals(1, searcher.search(d1, 10).totalHits);
    assertEquals(1, searcher.search(d2, 10).totalHits);
    assertEquals(1, searcher.search(d3, 10).totalHits);
    assertEquals(0, searcher.search(d99, 10).totalHits);
    assertEquals(3, searcher.search(bq234, 10).totalHits);
    assertEquals(2, searcher.search(bq19, 10).totalHits);
    assertEquals(2, searcher.search(bq26, 10).totalHits);


    // now test of references ( X --> (x))
    Map<String, Integer> scache = DictionaryRecIdCache.INSTANCE.getCache(DictionaryRecIdCache.Str2LuceneId.MAPPING, searcher, idField);
    Map<String, Integer> scache2 = DictionaryRecIdCache.INSTANCE.getCache(DictionaryRecIdCache.Str2LuceneId.MAPPING, searcher, idField);
    assertTrue(scache.hashCode() == scache2.hashCode());
    assertTrue(scache == scache2);


    assertEquals(4, searcher.search(new SecondOrderQuery(d1, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
    assertEquals(0, searcher.search(new SecondOrderQuery(d2, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
    assertEquals(1, searcher.search(new SecondOrderQuery(d3, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
    assertEquals(0, searcher.search(new SecondOrderQuery(d4, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
    assertEquals(1, searcher.search(new SecondOrderQuery(d5, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
    assertEquals(1, searcher.search(new SecondOrderQuery(d6, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
    assertEquals(1, searcher.search(new SecondOrderQuery(d7, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
    assertEquals(1, searcher.search(new SecondOrderQuery(d8, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
    assertEquals(4, searcher.search(new SecondOrderQuery(d9, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
    assertEquals(2, searcher.search(new SecondOrderQuery(d10, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
    assertEquals(0, searcher.search(new SecondOrderQuery(d99, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);



    int[][] invCache = DictionaryRecIdCache.INSTANCE.getCache(DictionaryRecIdCache.UnInvertedArray.MULTIVALUED_STRING, searcher, idField, refField);
    int[][] invCache2 = DictionaryRecIdCache.INSTANCE.getCache(DictionaryRecIdCache.UnInvertedArray.MULTIVALUED_STRING, searcher, idField, refField);
    assertTrue(invCache.equals(invCache2));
    assertTrue(invCache == invCache2);


    assertEquals(0, searcher.search(new SecondOrderQuery(d1, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
    assertEquals(2, searcher.search(new SecondOrderQuery(d2, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
    assertEquals(3, searcher.search(new SecondOrderQuery(d3, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
    assertEquals(3, searcher.search(new SecondOrderQuery(d4, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
    assertEquals(4, searcher.search(new SecondOrderQuery(d5, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
    assertEquals(0, searcher.search(new SecondOrderQuery(d6, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
    assertEquals(0, searcher.search(new SecondOrderQuery(d7, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
    assertEquals(0, searcher.search(new SecondOrderQuery(d8, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
    assertEquals(1, searcher.search(new SecondOrderQuery(d9, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
    assertEquals(2, searcher.search(new SecondOrderQuery(d10, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
    assertEquals(0, searcher.search(new SecondOrderQuery(d99, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);


    FieldCache.DEFAULT.purgeAllCaches();

    String boostField = "boost";
    String constBoost = "const_boost";
    assertEquals(4, searcher.search(new SecondOrderQuery(d1, null, new SecondOrderCollectorOperatorExpertsCiting(idField, refField, boostField)), 10).totalHits);
    assertEquals(4, searcher.search(new SecondOrderQuery(d1, null, new SecondOrderCollectorOperatorExpertsCiting(idField, refField, constBoost)), 10).totalHits);

    TopDocs normalSet = searcher.search(new SecondOrderQuery(d1, null, new SecondOrderCollectorOperatorExpertsCiting(idField, refField, boostField)), 10);
    TopDocs constSet = searcher.search(new SecondOrderQuery(d1, null, new SecondOrderCollectorOperatorExpertsCiting(idField, refField, constBoost)), 10);


    // because the query finds only one document, the order is basically unchanged (because it is the order of the 'source' 
    // paper that contributes to the weight of the found hits)
    assert normalSet.totalHits == constSet.totalHits;
    testDocOrder(constSet.scoreDocs, 2,3,4,5);
    testDocOrder(normalSet.scoreDocs, 2,3,4,5);
    assertArrayEquals(getIds(normalSet.scoreDocs), getIds(normalSet.scoreDocs));
    assertNotSame("The scores should be different", normalSet.getMaxScore(), constSet.getMaxScore());


    normalSet = searcher.search(new SecondOrderQuery(bq19, null, new SecondOrderCollectorOperatorExpertsCiting(idField, refField, boostField)), 10);
    constSet = searcher.search(new SecondOrderQuery(bq19, null, new SecondOrderCollectorOperatorExpertsCiting(idField, refField, constBoost)), 10);

    // expected order: 5,2,3,4,10
    assert normalSet.totalHits == constSet.totalHits;
    testDocOrder(constSet.scoreDocs, 2,3,4,5,10);
    testDocOrder(normalSet.scoreDocs, 5,2,3,4,10);
    assertNotSame("The scores should be different", normalSet.getMaxScore(), constSet.getMaxScore());


    
    // now we should find the same number of papers, but their order is to be different
    // because they are referenced (multiple times) and the new doc10 should raise up 
    // the score of docs 3,4
    
    normalSet = searcher.search(new SecondOrderQuery(bq1910, null, new SecondOrderCollectorOperatorExpertsCiting(idField, refField, boostField)), 10);
    constSet = searcher.search(new SecondOrderQuery(bq1910, null, new SecondOrderCollectorOperatorExpertsCiting(idField, refField, constBoost)), 10);


    // expected order: 5, 3, 4, 2, 10
    assert normalSet.totalHits == constSet.totalHits;
    testDocOrder(constSet.scoreDocs, 2,3,4,5,10);
    testDocOrder(normalSet.scoreDocs, 5,2,3,4,10);
    assertNotSame("The scores should be different", normalSet.getMaxScore(), constSet.getMaxScore());


    normalSet = searcher.search(new SecondOrderQuery(d5, null, new SecondOrderCollectorCitingTheMostCited(idField, refField, boostField)), 10);
    constSet = searcher.search(new SecondOrderQuery(d5, null, new SecondOrderCollectorCitingTheMostCited(idField, refField, constBoost)), 10);

    normalSet = searcher.search(new SecondOrderQuery(all, null, new SecondOrderCollectorCitingTheMostCited(idField, refField, boostField)), 10);
    constSet = searcher.search(new SecondOrderQuery(all, null, new SecondOrderCollectorCitingTheMostCited(idField, refField, constBoost)), 10);

    //System.out.println(normalSet);
    //System.out.println(normalSet);
    
    
    // topN()
    
    TopDocs topSet = searcher.search(new SecondOrderQuery(new MatchAllDocsQuery(), null, new SecondOrderCollectorTopN(2, true)), 10);
    testDocOrder(topSet.scoreDocs, 0, 1);
    
    topSet = searcher.search(new SecondOrderQuery(new MatchAllDocsQuery(), null, new SecondOrderCollectorTopN(3, false)), 10);
    testDocOrder(topSet.scoreDocs, 0, 1, 2);
    
    // returns: 2,3,4,5,10
    SecondOrderQuery constQuery = new SecondOrderQuery(bq1910, null, new SecondOrderCollectorOperatorExpertsCiting(idField, refField, constBoost));
    // returns: 5,2,3,4,10
    SecondOrderQuery boostQuery = new SecondOrderQuery(bq1910, null, new SecondOrderCollectorOperatorExpertsCiting(idField, refField, boostField));
    
    topSet = searcher.search(new SecondOrderQuery(constQuery, null, new SecondOrderCollectorTopN(3, false)), 10);
    testDocOrder(topSet.scoreDocs, 2, 3, 4);
    
    topSet = searcher.search(new SecondOrderQuery(boostQuery, null, new SecondOrderCollectorTopN(3, false)), 10);
    testDocOrder(topSet.scoreDocs, 5, 2, 3);
    
    topSet = searcher.search(new SecondOrderQuery(new TermQuery(new Term("id", "xxxx")), null, new SecondOrderCollectorTopN(3, false)), 10);
    assert topSet.totalHits == 0;
    
    
    // ADS Classic scoring formula
    topSet = searcher.search(new SecondOrderQuery(constQuery, null, new SecondOrderCollectorAdsClassicScoringFormula("ads_boost")), 10);
    testDocOrder(topSet.scoreDocs, 3, 2, 4, 10, 5);
    assertArrayEquals(getScores(topSet.scoreDocs), new float[]{0.95f, 0.55f, 0.55f, 0.55f, 0.5f}, 0.1f);
    
    
    
    // various algorithms for compacting the hits
    TermQuery kwq = new TermQuery(new Term("kw", "x"));
    TermQuery kwa = new TermQuery(new Term("ka", "a"));
    BooleanQuery bqa = new BooleanQuery();
    bqa.add(kwq, Occur.SHOULD);
    bqa.add(kwa, Occur.SHOULD);
    
    SecondOrderCollectorCites c = new SecondOrderCollectorCites(idField, refField);
    c.setFinalValueType(FinalValueType.ABS_COUNT);
    topSet = searcher.search(new SecondOrderQuery(bqa, null, c), 10);
    testDocOrder(topSet.scoreDocs, 15, 13, 12, 11);
    assertArrayEquals(getScores(topSet.scoreDocs), new float[]{5.0f, 3.0f, 2.0f, 1.0f}, 0.1f);
    
    c = new SecondOrderCollectorCites(idField, refField);
    c.setFinalValueType(FinalValueType.ABS_COUNT_NORM);
    topSet = searcher.search(new SecondOrderQuery(bqa, null, c), 10);
    testDocOrder(topSet.scoreDocs, 15, 13, 12, 11);
    assertArrayEquals(getScores(topSet.scoreDocs), new float[]{1.0f, 0.6f, 0.4f, 0.2f}, 0.1f);
    
    c = new SecondOrderCollectorCites(idField, refField);
    c.setFinalValueType(FinalValueType.MIN_VALUE);
    topSet = searcher.search(new SecondOrderQuery(bqa, null, c), 10);
    testDocOrder(topSet.scoreDocs, 11, 12, 13, 15);
    //assertArrayEquals(getScores(topSet.scoreDocs), new float[]{2.78f, 0.41f, 0.41f, 0.41f}, 0.1f);
    
    c = new SecondOrderCollectorCites(idField, refField);
    c.setFinalValueType(FinalValueType.MAX_VALUE);
    topSet = searcher.search(new SecondOrderQuery(bqa, null, c), 10);
    testDocOrder(topSet.scoreDocs, 11, 12, 13, 15);
    float tops = topSet.scoreDocs[0].score;
    for (ScoreDoc s: topSet.scoreDocs) {
    	//assertEquals(tops, s.score, 0.02);
    }
    //assertArrayEquals(getScores(topSet.scoreDocs), new float[]{1.98f, 1.98f, 1.98f, 1.98f}, 0.1f);
    
    System.out.println("gmean");
    c = new SecondOrderCollectorCites(idField, refField);
    c.setFinalValueType(FinalValueType.GEOM_MEAN);
    topSet = searcher.search(new SecondOrderQuery(bqa, null, c), 10);
    testDocOrder(topSet.scoreDocs, 11, 13, 15, 12);
    float[] gscores = getScores(topSet.scoreDocs);
    //assertArrayEquals(getScores(topSet.scoreDocs), new float[]{1.98f, 1.98f, 1.98f, 1.98f}, 0.1f);
    
    System.out.println("amean");
    c = new SecondOrderCollectorCites(idField, refField);
    c.setFinalValueType(FinalValueType.ARITHM_MEAN);
    topSet = searcher.search(new SecondOrderQuery(bqa, null, c), 10);
    testDocOrder(topSet.scoreDocs, 11, 13, 15, 12);
    float[] ascores = getScores(topSet.scoreDocs);
    
    //assertArrayEquals(getScores(topSet.scoreDocs), new float[]{1.98f, 1.98f, 1.98f, 1.98f}, 0.1f);
    
    System.out.println("gmean norm");
    c = new SecondOrderCollectorCites(idField, refField);
    c.setFinalValueType(FinalValueType.GEOM_MEAN_NORM);
    topSet = searcher.search(new SecondOrderQuery(bqa, null, c), 10);
    testDocOrder(topSet.scoreDocs, 11, 13, 15, 12);
    float[] gnscores = getScores(topSet.scoreDocs);
    
    System.out.println("anmean norm");
    c = new SecondOrderCollectorCites(idField, refField);
    c.setFinalValueType(FinalValueType.ARITHM_MEAN_NORM);
    topSet = searcher.search(new SecondOrderQuery(bqa, null, c), 10);
    testDocOrder(topSet.scoreDocs, 11, 13, 15, 12);
    float[] anscores = getScores(topSet.scoreDocs);
    //assertArrayEquals(getScores(topSet.scoreDocs), new float[]{1.98f, 1.98f, 1.98f, 1.98f}, 0.1f);
    
    assertTrue(ascores[1] != gscores[1]);
    assertTrue(ascores[2] != gscores[2]);
    assertTrue(anscores[0] == 1.0f);
    assertTrue(anscores[anscores.length-1] < ascores[ascores.length-1]);
    assertTrue(gnscores[0] < gscores[0]);
  }

  private int[] getIds(ScoreDoc[] docs) {
    int[] out = new int[docs.length];
    int i = 0;
    for (ScoreDoc d: docs) {
      out[i++] = d.doc;
    }
    return out;
  }

  private float[] getScores(ScoreDoc[] docs) {
    float[] out = new float[docs.length];
    int i = 0;
    for (ScoreDoc d: docs) {
      out[i++] = d.score;
    }
    return out;
  }

  // Uniquely for Junit 3
  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(TestSecondOrderQueryTypesAds.class);
  }
  
  /*
   * Test docs are in the given order (the order is not important
   * where the score is the same [for the given subset]
   */
  private void testDocOrder(ScoreDoc[] docs, int...expected) {
    float lastScore = docs[0].score;
    
    int[] ids = getIds(docs);
    float[] scores = getScores(docs);
    HashMap<Float, List<Integer>> scoreMap = new HashMap<Float, List<Integer>>();
    for (int i=0;i<ids.length;i++) {
      float s = scores[i];
      if (!scoreMap.containsKey(s)) {
        scoreMap.put(s, new ArrayList<Integer>());
      }
      scoreMap.get(s).add(ids[i]);
    }
    
    for (int i=0;i<expected.length;i++) {
      ScoreDoc doc = docs[i];
      System.out.println(doc);
      
      if (lastScore != doc.score) {
        scoreMap.remove(lastScore);
        lastScore = doc.score;
      }
      
      if (!scoreMap.get(lastScore).contains((Integer)doc.doc)) {
        fail("The document on position " + i + " is not " + expected[i] + ", we want: " + doc.doc);
      }
      
      
    }
  }
}