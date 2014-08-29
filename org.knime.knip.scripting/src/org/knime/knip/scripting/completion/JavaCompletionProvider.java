package org.knime.knip.scripting.completion;


import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.FunctionCompletion;
import org.fife.ui.autocomplete.ParameterizedCompletion.Parameter;

public class JavaCompletionProvider extends DefaultCompletionProvider {

	public JavaCompletionProvider() {
		super();
	}


	public void updateCompletions(String compilationUnit) {
		try {
			updateCompletions(JavaParser.parse(new ByteArrayInputStream(compilationUnit.getBytes(StandardCharsets.UTF_8))));
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	/**
	 */
	public void updateCompletions(CompilationUnit u) {
		
		for (ImportDeclaration i : u.getImports()) {
			Package p = Package.getPackage(i.getName().toString());
			
			if (p == null) {
				System.out.println("Failed " + i.getName());
				continue;
			}
			for (Package subp : p.getPackages()) {
				addCompletion(new ImportCompletion(this, subp));
			}
		}
		
//		for (Method m : u.getTypes()) {
//			FunctionCompletion compl = new FunctionCompletion(this, m.getName(), m.getReturnType().getName());
//			
//			ArrayList<Parameter> params = new ArrayList<Parameter>();
//			int i = 0;
//			for (Class<?> type : m.getParameterTypes()) {
//				params.add(new Parameter(type, "arg" + i));
//			}
//			compl.setParams(params);
//			
//			this.addCompletion(compl);
//		}
	}
}
