package core.stereotype.stereotyped;

import core.stereotype.taxonomy.CodeStereotype;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.Javadoc;

import java.util.List;

public interface StereotypedElement {
	BodyDeclaration getElement();

	List<CodeStereotype> getStereotypes();

	List<StereotypedElement> getStereoSubElements();

	void findStereotypes();

	String getReport();

	Javadoc getJavadoc();

	ChildPropertyDescriptor getJavadocDescriptor();

	String getName();

	String getQualifiedName();
	
	String getFullyQualifiedName();

	String getKey();
}
