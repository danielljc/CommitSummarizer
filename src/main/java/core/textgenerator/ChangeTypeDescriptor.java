package core.textgenerator;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import core.Enums.Constants;

public class ChangeTypeDescriptor {

	public ChangeTypeDescriptor() {
		
	}
	
	public String generateDescription(ChangeType type) {
		return type.name().toLowerCase().replace(Constants.UNDERSCORE, Constants.SPACE);
	}

}
