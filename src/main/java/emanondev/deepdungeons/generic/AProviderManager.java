package emanondev.deepdungeons.generic;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import emanondev.core.UtilsString;

public class AProviderManager<T extends Provider> implements ProviderManager<T> {

	private final HashMap<String,T> providers = new HashMap<>();
	
	public void register(T provider) {
		if (providers.containsKey(provider.getId()))
			throw new IllegalStateException("Duplicate id");
		if (!UtilsString.isLowcasedValidID(provider.getId()))
			throw new IllegalStateException("Invalid id");
		providers.put(provider.getId(), provider);
	}
	
	public boolean unregister(T provider) {
		return providers.remove(provider.getId(),provider);
	}
	
	public Set<String> getProviderIds(){
		return Collections.unmodifiableSet(providers.keySet());
	}
	
	public Collection<T> getProviders(){
		return Collections.unmodifiableCollection(providers.values());
	}
	
	public T getProvider(String id) {
		return providers.get(id);
	}
}
