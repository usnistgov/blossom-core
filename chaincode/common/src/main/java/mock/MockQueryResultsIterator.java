package mock;

import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.Iterator;
import java.util.List;

public class MockQueryResultsIterator<T> implements QueryResultsIterator<T> {

    private List<T> results;

    public MockQueryResultsIterator(List<T> results) {
        this.results = results;
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public Iterator<T> iterator() {
        return results.iterator();
    }
}
