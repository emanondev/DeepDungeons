package emanondev.deepdungeons.generic;

import emanondev.core.UtilsString;

public class AProvider implements Provider {

	private final String id;

	public AProvider(String id) {
		if (id==null)
			throw new NullPointerException();
		if (!UtilsString.isLowcasedValidID(id))
			throw new IllegalArgumentException("Invalid id");
		this.id = id.toLowerCase();
	}

	@Override
	public final String getId() {
		return id;
	}

}
