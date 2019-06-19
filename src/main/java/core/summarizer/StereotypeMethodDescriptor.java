package core.summarizer;

import core.Enums.Constants;
import core.stereotype.stereotyped.StereotypedElement;
import core.textgenerator.phrase.MethodPhraseGenerator;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.List;

public class StereotypeMethodDescriptor {
	
	public static String describe(List<StereotypedElement> element) {
		String description = Constants.EMPTY_STRING;
		int i = 0;
		for (StereotypedElement method : element) {
			if(method.getElement() instanceof MethodDeclaration) {
				MethodPhraseGenerator phraseGenerator = new MethodPhraseGenerator(method, "BASIC");
				phraseGenerator.generate();
				if(!description.contains(phraseGenerator.getPhrase())) {
					String phrase = (i == (element.size() - 1)) ? phraseGenerator.getPhrase().replace(";", Constants.EMPTY_STRING) : phraseGenerator.getPhrase();
					description += phrase; 
				}	
				
			}
			i++;
		}
		return description;
	}
}
