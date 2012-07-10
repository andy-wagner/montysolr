package org.apache.lucene.queryparser.flexible.aqp.builders;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.flexible.messages.MessageImpl;
import org.apache.lucene.queryparser.flexible.aqp.AqpSubqueryParser;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.search.Query;
import org.apache.solr.search.AqpFunctionQParser;

public class AqpSubQueryTreeBuilder extends QueryTreeBuilder {
	
	private AqpSubqueryParser vs;
	private AqpFunctionQParser fp;
	
	public AqpSubQueryTreeBuilder(AqpSubqueryParser provider, AqpFunctionQParser parser) {
		vs = provider;
		fp = parser;
	}
	
	public Query build(QueryNode node) throws QueryNodeException {
		try {
			fp.setQueryNode(node);
			return vs.parse(fp);
		} catch (ParseException e) {
			throw new QueryNodeException(new MessageImpl(e.getLocalizedMessage()));
		}
	}
}