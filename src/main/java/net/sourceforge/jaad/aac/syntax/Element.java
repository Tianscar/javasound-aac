package net.sourceforge.jaad.aac.syntax;

import net.sourceforge.jaad.aac.DecoderConfig;

import java.util.*;
import java.util.function.IntFunction;

public interface Element {

	enum Type {
		SCE, CPE, CCE, LFE, DSE, PCE, FIL, END;
		public static final List<Type> VALUES = List.of(values());
		public static Type get(int i) {return VALUES.get(i);}
	}

	static Type readType(BitStream in) {
		return Type.get(in.readBits(3));
	}

	abstract class InstanceTag {

		protected final int id;

		protected InstanceTag(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		abstract public Type getType();

		abstract public Element newElement(DecoderConfig config);

		public int hashCode() {
			return getType().ordinal() + 8*id;
		}

		public boolean equals(Object obj) {
			return this.getClass().equals(obj.getClass()) && equalsTag((InstanceTag)obj);
		}

		boolean equalsTag(InstanceTag other) {
			return this.id == other.id && Objects.equals(getId(), other.getId());
		}

		transient String toString;

		public String toString() {
			if(toString==null)
				toString = String.format("%s:[%d]", getType().name(), id);
			return toString;
		}
	}

	InstanceTag getElementInstanceTag();

	void decode(BitStream in);

	static <T extends InstanceTag> List<T>
	createTagList(int count, IntFunction<T> newTag) {
		List<T> tags = new AbstractList<>() {

			@Override
			public int size() {
				return count;
			}

			@Override
			public T get(int index) {
				return newTag.apply(index);
			}
		};
		return List.copyOf(tags);
	}

	static Map<Type, IntFunction<InstanceTag>> tagFactory() {
		Map<Type, IntFunction<InstanceTag>> types = new EnumMap<>(Element.Type.class);
		types.put(Element.Type.PCE, PCE.TAGS::get);
		types.put(Element.Type.SCE, SCE.TAGS::get);
		types.put(Element.Type.CPE, CPE.TAGS::get);
		types.put(Element.Type.LFE, LFE.TAGS::get);
		types.put(Element.Type.CCE, CCE.TAGS::get);
		types.put(Element.Type.DSE, DSE.TAGS::get);
		return types;
	}
}

