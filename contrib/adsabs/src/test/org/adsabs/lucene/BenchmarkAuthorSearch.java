package org.adsabs.lucene;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.StorableField;
import org.apache.lucene.index.StoredDocument;
import org.apache.lucene.index.Term;
import org.apache.lucene.sandbox.queries.regex.RegexQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanNearPayloadCheckQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util._TestUtil;

public class BenchmarkAuthorSearch extends LuceneTestCase{
	private IndexSearcher searcher;
	private IndexReader reader;
	private Directory dir;
	private int numDocs = 100000;
	private int numQueries = 1000;
	private boolean store = false;
	private ArrayList<ArrayList<Object>> timerStack = new ArrayList<ArrayList<Object>>();
	
	private String[] names = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "x",
			"john", "jay", "giovanni", "alberto", "edwin", "michael"};
	
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		reader.close();
		dir.close();
		
		assertTrue(timerStack.size()==0);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		startTimer("Buiding index of " + numDocs + " docs");
		
		dir = newDirectory();
		RandomIndexWriter writer = new RandomIndexWriter(random(), dir,
				newIndexWriterConfig(TEST_VERSION_CURRENT, new MultiFieldAnalyzer())
				.setMaxBufferedDocs(_TestUtil.nextInt(random(), 50, 1000)));

		Document doc = new Document();
		FieldType customType = new FieldType(TextField.TYPE_STORED);
		customType.setOmitNorms(true);
		
		Field id = newField("id", "", store ? StringField.TYPE_STORED : StringField.TYPE_NOT_STORED);
		Field original = newField("original", "", StringField.TYPE_STORED);
		Field regex = newField("regex", "", store ? StringField.TYPE_STORED : StringField.TYPE_NOT_STORED);
		Field wildcard = newField("wildcard", "", store ? StringField.TYPE_STORED : StringField.TYPE_NOT_STORED);
		Field payload = newField("payload", "", store ? TextField.TYPE_STORED : TextField.TYPE_NOT_STORED);
		Field n0 = newField("n0", "", store ? StringField.TYPE_STORED : StringField.TYPE_NOT_STORED);
		Field n1 = newField("n1", "", store ? StringField.TYPE_STORED : StringField.TYPE_NOT_STORED);
		Field n2 = newField("n2", "", store ? StringField.TYPE_STORED : StringField.TYPE_NOT_STORED);
		Field n3 = newField("n3", "", store ? StringField.TYPE_STORED : StringField.TYPE_NOT_STORED);
		Field n4 = newField("n4", "", store ? StringField.TYPE_STORED : StringField.TYPE_NOT_STORED);

		doc.add(id);
		doc.add(original);
		doc.add(regex);
		doc.add(wildcard);
		doc.add(payload);
		doc.add(n0);
		doc.add(n1);
		doc.add(n2);
		doc.add(n3);
		doc.add(n4);
		
		Field[] nFields = {n0, n1, n2, n3, n4};
		Field[] myFields = {id,original, regex, wildcard, payload, n0, n1, n2, n3, n4};
		String surname;
		
		for (int i = 0; i < numDocs; i++) {
			for (Field f: myFields) {
				f.setStringValue("");
			}
			
			StringBuilder name = new StringBuilder();
			StringBuilder wild = new StringBuilder();
			
		  //surname
		  do {
	      surname = _TestUtil.randomSimpleString(random()).toLowerCase().replace(",", "").trim();
		  } while (surname.length() == 0);
		  
	      name.append(surname);
	      name.append(", ");
	      wild.append(surname);
	      wild.append(", ");
	      n0.setStringValue(surname);
	      
	      //#initials
	      int noi = _TestUtil.nextInt(random(), 0, 4);
	      for (int j = 0; j < noi; j++) {
	    	  String namePart = names[_TestUtil.nextInt(random(), 0, names.length-1)];
	    	  name.append(namePart);
	    	  name.append(" ");
	    	  wild.append(namePart);
	    	  wild.append(j+1);
	    	  wild.append(" ");
	    	  nFields[j+1].setStringValue(namePart);
	      }
	      
	      original.setStringValue(name.toString());
	      regex.setStringValue(name.toString());
	      wildcard.setStringValue(wild.toString());
	      payload.setStringValue(name.toString());
	      id.setStringValue(Integer.toString(i));
	      
	      writer.addDocument(doc);
	    }
	

		reader = writer.getReader();
		writer.close();
		searcher = newSearcher(reader);
		
		stopTimer();
	}

	
	private void startTimer(String message) {
		ArrayList<Object> l = new ArrayList<Object>();
		l.add(System.currentTimeMillis());
		l.add(message);
		timerStack.add(l);
	}
	
	private long stopTimer() {
		ArrayList<Object> l = timerStack.remove(timerStack.size()-1);
		long endTime = System.currentTimeMillis();
		long startTime = (Long) l.get(0);
		String msg = (String) l.get(1);
		
		long resTime = endTime - startTime;
		
		StringBuilder out = new StringBuilder();
		for (int i=0;i<timerStack.size();i++) {
			out.append("  ");
		}
		out.append(resTime);
		out.append("ms.  " + msg);
		System.out.println(out.toString());
		
		return resTime;
	}
	
    /**
     * This Analyzer uses an WhitespaceTokenizer and PayloadFilter, OR KeywordTokenizer for
     * other queries
     */
    private static class MultiFieldAnalyzer extends Analyzer {

        public MultiFieldAnalyzer() {
          super(new PerFieldReuseStrategy());
        }
        
        public MultiFieldAnalyzer(String field, byte[] data, int offset, int length) {
            super(new PerFieldReuseStrategy());
        }

        
        @Override
        public TokenStreamComponents createComponents(String fieldName, Reader reader) {
        	
        	if (fieldName.contains("payload")){
				Tokenizer result = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
				return new TokenStreamComponents(result, new SimplePayloadFilter(result));
			}
        	
        	Tokenizer result = new MockTokenizer(reader, MockTokenizer.KEYWORD, true);
			return new TokenStreamComponents(result, result);
        	
        }
        
    }

    
    /**
     * This Filter adds payloads to the tokens.
     */
    static final class SimplePayloadFilter extends TokenFilter {
        int pos;
        final PayloadAttribute payloadAttr;
        final CharTermAttribute termAttr;

        public SimplePayloadFilter(TokenStream input) {
          super(input);
          pos = 0;
          payloadAttr = input.addAttribute(PayloadAttribute.class);
          termAttr = input.addAttribute(CharTermAttribute.class);
        }

        @Override
        public boolean incrementToken() throws IOException {
          if (input.incrementToken()) {
        	  if (pos == 0 && termAttr.toString().endsWith(",")) {
        		  termAttr.setEmpty().append(input.toString().replace(",", ""));
        	  }
            payloadAttr.setPayload(new BytesRef(("pos: " + pos).getBytes("UTF-8")));
            pos++;
            return true;
          } else {
            return false;
          }
        }

        @Override
        public void reset() throws IOException {
          super.reset();
          pos = 0;
        }
      }
    
    public void testBenchMarkAll() throws IOException {
    	
    	
    	int[] randomIds = getRandomIds(100);
    	
    	startTimer("Verifying data integrity with " + randomIds.length + " docs");
    	verifySearch(randomIds);
    	stopTimer();
    	
    	startTimer("Preparing " + numQueries + " random queries");
    	randomIds = getRandomIds(numQueries);
    	List<TestCase> testCases = getIndexData(randomIds);
    	stopTimer();
    	
    	ArrayList<Integer> totals = new ArrayList<Integer>();
    	
    	startTimer("Regex queries");
    	totals.add(runRegexQueries(testCases));
    	stopTimer();
    	
    	startTimer("Regexp queries (new style)");
    	totals.add(runRegexQueries(testCases));
    	stopTimer();
    	
    	startTimer("Wildcard queries");
    	totals.add(runWildcardQueries(testCases));
    	stopTimer();
    	
    	startTimer("Boolean queries");
    	totals.add(runBooleanQueries(testCases));
    	stopTimer();
    	
    	
    	System.out.println("Totals: " + totals);
    }
    
    private int runRegexQueries(List<TestCase> testCases) throws IOException {
    	int total = 0;
		for (TestCase t: testCases) {
			total  += searcher.search(getRegexQuery(t.parts, t.howMany, t.truncate), 1).totalHits;
		}
		return total;
	}
    
    private int runRegexpQueries(List<TestCase> testCases) throws IOException {
    	int total = 0;
		for (TestCase t: testCases) {
			total  += searcher.search(getRegexpQuery(t.parts, t.howMany, t.truncate), 1).totalHits;
		}
		return total;
	}

    private int runWildcardQueries(List<TestCase> testCases) throws IOException {
    	int total = 0;
		for (TestCase t: testCases) {
			total  += searcher.search(getWildcardQuery(t.parts, t.howMany, t.truncate), 1).totalHits;
		}
		return total;
	}
    
    private int runBooleanQueries(List<TestCase> testCases) throws IOException {
    	int total = 0;
		for (TestCase t: testCases) {
			total  += searcher.search(getBooleanQuery(t.parts, t.howMany, t.truncate), 1).totalHits;
		}
		return total;
	}
    
	class TestCase {
    	public String original;
    	public String[] parts;
    	public int howMany;
    	public boolean truncate = false;

		TestCase(String original, String[] parts, int howMany) {
    		this.original = original;
    		this.parts = parts;
    		this.howMany = howMany;
    	}
    }
	private List<TestCase> getIndexData(int[] randomIds) throws IOException {
		ArrayList<TestCase> data = new ArrayList<TestCase>(randomIds.length);
		for (int i = 0; i < randomIds.length; i++) {
			TopDocs docs = searcher.search(new TermQuery(new Term("id", Integer.toString(randomIds[i]))), 1);
			StoredDocument doc = reader.document(docs.scoreDocs[0].doc);
			String original = doc.get("original").toString();
			String[] parts = original.split("\\,? ");
			int howMany = _TestUtil.nextInt(random(), 0, parts.length-1); // how many initials
			data.add(new TestCase(original, parts, howMany));
		}
		return data;
	}

	private void verifySearch(int[] randomIds) throws IOException {
		for (int i = 0; i < randomIds.length; i++) {
			TopDocs docs = searcher.search(new TermQuery(new Term("id", Integer.toString(randomIds[i]))), 1);
			if (docs.totalHits == 1) {
				StoredDocument doc = reader.document(docs.scoreDocs[0].doc);
				String original = doc.getField("original").stringValue();
				String[] parts = original.split("\\,? ");
				Query[] queries = buildQueries(parts);
				TermQuery oq = new TermQuery(new Term("original", original));
				int ho = searcher.search(oq, 1).totalHits;
				for (Query q: queries) {
					BooleanQuery bq = new BooleanQuery();
					bq.add(q, Occur.MUST);
					bq.add(new TermQuery(new Term("id", Integer.toString(randomIds[i]))), Occur.MUST);
					if (q != null) {
						int no = searcher.search(bq, 1).totalHits;
						if (no != 1) {
							System.out.println("Results differ: " + oq + " <<>> " + q + "   [" + ho + " : " + no + "]");
							if (store == true) {
								System.out.println("wildcard: \"" + doc.getField("wildcard").stringValue()  + "\"");
								System.out.println("regex: \"" + doc.getField("regex").stringValue() + "\"");
								System.out.println("payload: \"" + doc.getField("payload").stringValue() + "\"");
								System.out.println("n0: \"" + doc.getField("n0").stringValue() + "\"");
								System.out.println("n1: \"" + doc.getField("n1").stringValue() + "\"");
								System.out.println("n2: \"" + doc.getField("n2").stringValue() + "\"");
								System.out.println("n3: \"" + doc.getField("n3").stringValue() + "\"");
								System.out.println("n4: \"" + doc.getField("n4").stringValue() + "\"");
							}
						}
						//assertEquals(ho, no);
					}
				}
			}
		}
		
	}

	private Query[] buildQueries(String[] parts) throws UnsupportedEncodingException {
		int howMany = _TestUtil.nextInt(random(), 0, parts.length-1); // how many initials
		Query[] queries = new Query[5];
		queries[0] = getRegexQuery(parts, howMany, false);
		queries[1] = getRegexpQuery(parts, howMany, false);
		queries[2] = getWildcardQuery(parts, howMany, false);
		queries[3] = getBooleanQuery(parts, howMany, false);
		//queries[4] = getPayloadQuery(parts, howMany, false);
		
		return queries;
	}

	private Query getPayloadQuery(String[] parts, int howMany, boolean truncate) throws UnsupportedEncodingException {
		Collection<byte[]> payloads = new ArrayList<byte[]>(howMany+1);
	    BytesRef pay = new BytesRef(("pos: " + 0).getBytes("UTF-8"));
	    payloads.add(pay.bytes);
	    
		SpanQuery[] clauses = new SpanQuery[howMany+1];
		clauses[0] = new SpanTermQuery(new Term("payload", parts[0])); // surname
		for (int i = 1; i < howMany; i++) {
			clauses[i] = new SpanTermQuery(new Term("payload", parts[i])); // TODO: make it search for prefix
			pay = new BytesRef(("pos: " + i).getBytes("UTF-8"));
			payloads.add(pay.bytes);
		}
		SpanNearQuery sq = new SpanNearQuery(clauses, 1, true); // match in order
		return new SpanNearPayloadCheckQuery(sq, payloads);
	}

	private Query getBooleanQuery(String[] parts, int howMany, boolean truncate) {
		BooleanQuery bq = new BooleanQuery();
		bq.add(new TermQuery(new Term("n0", parts[0])), BooleanClause.Occur.MUST);
		for (int i = 1; i < howMany+1; i++) {
			bq.add(new TermQuery(new Term("n"+i, parts[i])), BooleanClause.Occur.MUST);
		}
		return bq;
	}

	private Query getWildcardQuery(String[] parts, int howMany, boolean truncate) {
		return new WildcardQuery(new Term("wildcard", getWildcardQueryString(parts, howMany, truncate)));
	}
	
	private String getWildcardQueryString(String[] parts, int howMany, boolean truncate) {
		StringBuilder p = new StringBuilder();
		p.append(parts[0]);
		p.append(", ");
		int i = 0;
		for (; i < howMany && parts.length > i; i++) {
			String x = truncate ? parts[i+1].substring(0, 1) : parts[i+1];
			p.append(x + "*" + (i+1) + " ");
		}
		if (parts.length > i) {
			p.append("*");
		}
		return p.toString();
	}

	private Query getRegexQuery(String[] parts, int howMany, boolean truncate) {
		return new RegexQuery(new Term("regex", getRegexQueryString(parts, howMany, truncate)));
	}
	
	private Query getRegexpQuery(String[] parts, int howMany, boolean truncate) {
		return new RegexpQuery(new Term("regex", getRegexQueryString(parts, howMany, truncate)));
	}
	
	private String getRegexQueryString(String[] parts, int howMany, boolean truncate) {
		StringBuilder p = new StringBuilder();
		p.append(parts[0]);
		p.append(", ");
		int i = 0;
		for (; i < howMany && parts.length > i; i++) {
			String x = truncate ? parts[i+1].substring(0, 1) : parts[i+1];
			p.append(x + "\\w* ");
		}
		if (parts.length > i) {
			p.append(".*");
		}
		return p.toString();
	}

	private int[] getRandomIds(int i) {
		int[] randomIds = new int[Math.min(numDocs, i)];
		for (int j = 0; j < randomIds.length; j++) {
			randomIds[j] = _TestUtil.nextInt(random(), 0, numDocs);
		}
		return randomIds;
	}
    
    
}
