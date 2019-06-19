package core.stereotype.analyzer;

import core.stereotype.stereotyped.StereotypedMethod;
import core.visitor.TypeVisitor;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.LinkedList;
import java.util.List;

public class TypeAnalyzer {
	private List<StereotypedMethod> stereotypedMethods;
	private StringBuilder report;

	public TypeAnalyzer(final TypeDeclaration type) {
		super();
		this.stereotypedMethods = new LinkedList<StereotypedMethod>();
		this.report = new StringBuilder();
		final TypeVisitor visitor = new TypeVisitor(this);
		if(type != null && visitor != null) {
			type.accept((ASTVisitor) visitor);
		}
	}

	public List<StereotypedMethod> getStereotypedMethods() {
		return this.stereotypedMethods;
	}

	public StringBuilder getReport() {
		return this.report;
	}

}