package emanondev.deepdungeons.parameter;

import java.util.List;

import emanondev.core.UtilsString;
import emanondev.core.gui.GuiButton;
import emanondev.deepdungeons.generic.StandGui;

public abstract class Parameter<T> {
	
	public final String name;
	public final String nameS;
	public final T defaultValue;
	
	public Parameter(String name,T defaultValue){
		if (name==null)
			throw new NullPointerException();
		if (!UtilsString.isValidID(name))
			throw new NullPointerException();
		this.name = name;
		this.nameS = name+" ";
		this.defaultValue = defaultValue;
	}

	public abstract String toString(T value);
	
	public abstract T fromString(String value);
	
	public boolean isDefault(T value) {
		if (value==defaultValue)
			return true;
		if (defaultValue==null)
			return false;
		return defaultValue.equals(value);
	}
	
	public final boolean matchLine(String text) {
		return text.startsWith(nameS);
	}
	
	public T readValue(List<String> text) {
		if (text==null)
			return defaultValue;
		for (String line:text)
			if (matchLine(line))
				return fromString(line.substring(name.length()+1));
		
		return defaultValue;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Parameter other = (Parameter) obj;
		if (name == null) {
			return other.name == null;
		} else return name.equals(other.name);
	}

	public final void addValue(List<String> info,T value) {
		if (!isDefault(value))
			info.add(nameS+toString(value));
	}

	public abstract GuiButton getEditorButton(StandGui gui);

}
