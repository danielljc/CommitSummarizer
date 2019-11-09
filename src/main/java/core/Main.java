package core;

import core.Enums.Constants;
import core.git.ChangedFile;
import core.git.GitException;
import core.git.RepositoryHistory;
import core.git.SCMRepository;
import core.summarizer.SummarizeChanges;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.NoWorkTreeException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Main {
	
	private static final String REPOSITORY = "repository";
	private static final String OUTPUT = "output";
	private static final String FILTER_FACTOR = "filterFactor";
	private static final String OLDER_VERSION_ID = "olderVersionId";
	private static final String NEWER_VERSION_ID= "newerVersionId";
	
	private static SCMRepository repo ;
	private static Set<ChangedFile> differences;
	private static Git git;
	private static String projectPath = "/home/fernando/git/test/";
	private static String[] parameters = {"repository", "output", "filterFactor", "olderVersionId", "newerVersionId"};
	private static String olderVersionId;
	private static String newerVersionId;
	private static String outputFile;
	private static double filterFactor; 
	
	private static boolean readParams(String[] args) {
		boolean isValid = true;
		for (String string : args) {
			isValid = validateParam(string);
			if(Boolean.FALSE == isValid) {
				break;
			} else {
				assignValue(string);
			}
		}
		
		return isValid;
	}

	//该方法可能就是更改的关键，projectPath的赋值就在此
	private static void assignValue(String string) {
		String [] param = string.split(Constants.EQUAL);
		if(param[0].equals(REPOSITORY)) {
			projectPath = param[1];
		}
		
		if(param[0].equals(OUTPUT)) {
			outputFile = param[1];
		}
		
		if(param[0].equals(FILTER_FACTOR)) {
			try {
				filterFactor = Double.parseDouble(param[1]);
			} catch (NumberFormatException e) {
		        System.err.format("Argument %s must be a double value", param[0]);
		        System.exit(1);
		    }
		}
		
		if(param[0].equals(NEWER_VERSION_ID)) {
			newerVersionId = param[1];
		}
		
		if(param[0].equals(OLDER_VERSION_ID)) {
			olderVersionId = param[1];
		}
	}

	private static boolean validateParam(String string) {
		boolean isValid = true;
		List<String> params = Arrays.asList(parameters);
		String []split = string.split(Constants.EQUAL);
		
		if(!string.contains(Constants.EQUAL)) {
			System.err.println("The parameter format should be contains the =");
			isValid = false;
		} 
		
		if(null != split && split.length != 2) {
			System.err.println("The parameter format should be param=value");
			isValid = false;
		}
		
		if(split[0] != null && !params.contains(split[0])) {
			System.err.format("The parameter %s is not valid", split[0]);
			isValid = false;
		}
		
		return isValid;
	}

	private static IStatus gettingRepositoryStatus() {
		git = repo.getGit();

		if(git != null) {
			Status status = null;
			try {
				status = repo.getStatus();
			} catch (NoWorkTreeException e) {
				e.printStackTrace();
			} catch (GitAPIException e) {
				e.printStackTrace();
			} catch (final GitException e) {
				e.printStackTrace();
			}

			System.out.println("Extracting source code differences !");
			//这一句应该就是需要的代码，考虑删掉前面部分可能带来的影响？
			//这里的第二个参数怎么直接转化为我们的diff文件路径？
			differences = SCMRepository.getDifferences(status,git.getRepository().getWorkTree().getAbsolutePath());

		} else {
			System.err.println("Git repository not found!");
			return org.eclipse.core.runtime.Status.CANCEL_STATUS;
		}
		return org.eclipse.core.runtime.Status.OK_STATUS;
	}

	public static void main(String[] args) {
		if(null == args || args.length == 0 || !readParams(args)) {
			System.err.println("Error in the input parameters");
			return;
		}
		try {
			repo = new SCMRepository(projectPath);

			//该段代码中有用的大概就是
			// differences = SCMRepository.getDifferences(status,git.getRepository().getWorkTree().getAbsolutePath());
			gettingRepositoryStatus();

			//此段代码没有明显的作用，主要是根据获得的git对象执行一些我们不关心的操作？
			RepositoryHistory.getRepositoryHistory(git);

			//核心在于如何让git=null时下面的代码依旧能根据给出的文件路径名来生成changes
			//或者如何构造一个装载了需要的AB文件的Git对象？
			//关键的参数有两个，一个是git中含有的File相关属性，另一个是filterFactor
			SummarizeChanges summarizer = new SummarizeChanges(git, false, filterFactor, olderVersionId, newerVersionId);
			summarizer.setProjectPath(projectPath);
			if(null != differences && differences.size() > 0) {
				ChangedFile [] changes = new ChangedFile[differences.size()];
				summarizer.summarize(differences.toArray(changes));
			}
			File output = new File(outputFile);
			FileUtils.writeStringToFile(output, summarizer.getSummary());
		} catch (RuntimeException e1) {
			System.err.println("Not found a repository in the path " + projectPath);
		} catch (IOException e) {
			System.err.println("The output file can not be created in " + outputFile);
		} catch (NoHeadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
}
