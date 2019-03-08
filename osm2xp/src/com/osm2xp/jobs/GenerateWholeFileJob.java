package com.osm2xp.jobs;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.osm2xp.core.exceptions.DataSinkException;
import com.osm2xp.core.logging.Osm2xpLogger;
import com.osm2xp.core.parsers.IParser;
import com.osm2xp.parsers.builders.ParserBuilder;
import com.osm2xp.translators.ITranslator;

/**
 * Job for generating scenario for whole file, for non-tiled formats
 * 
 * @author Dmitry Karpenko
 * 
 */
public class GenerateWholeFileJob extends GenerateJob {

	private ITranslator translator;

	public GenerateWholeFileJob(File currentFile, String folderPath, ITranslator translator) {
		super("Generation started for " + currentFile.getName(), currentFile, folderPath, "todoJob");
		this.translator = translator;
		Osm2xpLogger.info("Starting  generation of several tiles, target folder " + folderPath);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			IParser parser = ParserBuilder.getParser(currentFile, translator);
			parser.process();
			Osm2xpLogger.info("Finished generation, target folder " + folderPath);
		} catch (DataSinkException e) {
			Osm2xpLogger.error("Data sink exception : ", e);
		} 

		return Status.OK_STATUS;

	}

}
