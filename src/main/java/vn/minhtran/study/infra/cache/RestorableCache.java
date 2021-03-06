package vn.minhtran.study.infra.cache;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;

public abstract class RestorableCache<K extends Serializable, V extends KeyEntity<K>> {

	private Map<K, V> store = new ConcurrentHashMap<>();

	protected V put(V value) {
		store.put(value.getKey(), value);
		executor.execute(() -> {
			getRepository().save(value);
		});
		return value;
	}

	protected boolean exist(K key) {
		return store.containsKey(key);
	}

	protected abstract CrudRepository<V, K> getRepository();

	@Autowired
	@Qualifier("datasourceSynchronizerExecutor")
	private Executor executor;

	protected void restore() {
		try {
			Iterable<V> all = getRepository().findAll();
			all.forEach(s -> {
				store.put(s.getKey(), s);
			});
		} catch (Exception e) {
		}
	}

	protected V get(K key) {
		return store.get(key);
	}

}
