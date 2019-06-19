package core.textgenerator.phrase;

import core.textgenerator.pos.TaggedTerm;

import java.util.LinkedList;

public abstract class Phrase {
	protected LinkedList<TaggedTerm> taggedPhrase;

	protected Phrase() {
		super();
	}

	protected Phrase(final LinkedList<TaggedTerm> taggedPhrase) {
		super();
		this.taggedPhrase = taggedPhrase;
	}

	public abstract void generate();

	public abstract String toString();
}
