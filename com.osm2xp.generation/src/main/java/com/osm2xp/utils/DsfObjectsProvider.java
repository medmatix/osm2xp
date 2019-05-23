package com.osm2xp.utils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.osm2xp.core.exceptions.Osm2xpBusinessException;
import com.osm2xp.core.logging.Osm2xpLogger;
import com.osm2xp.core.model.osm.Tag;
import com.osm2xp.generation.options.ObjectFile;
import com.osm2xp.generation.options.Polygon;
import com.osm2xp.generation.options.XPlaneOptionsProvider;
import com.osm2xp.generation.options.rules.FacadeTagRule;
import com.osm2xp.generation.options.rules.ForestTagRule;
import com.osm2xp.generation.options.rules.PolygonTagsRule;
import com.osm2xp.generation.options.rules.TagsRule;
import com.osm2xp.generation.options.rules.XplaneLightTagRule;
import com.osm2xp.generation.options.rules.XplaneObjectTagRule;
import com.osm2xp.generation.paths.PathsService;
import com.osm2xp.model.facades.Facade;
import com.osm2xp.model.facades.FacadeSetManager;
import com.osm2xp.model.facades.SpecialFacadeType;
import com.osm2xp.model.osm.polygon.OsmPolygon;
import com.osm2xp.model.osm.polygon.OsmPolyline;
import com.osm2xp.model.xplane.XplaneDsf3DObject;
import com.osm2xp.model.xplane.XplaneDsfLightObject;
import com.osm2xp.model.xplane.XplaneDsfObject;
import com.osm2xp.translators.BuildingType;
import com.osm2xp.utils.geometry.GeomUtils;
import com.osm2xp.utils.osm.OsmUtils;

import math.geom2d.polygon.LinearRing2D;

/**
 * DsfObjectsProvider.
 * 
 * @author Benjamin Blanchet
 * 
 */
public class DsfObjectsProvider {

	private static final String FACADES_DIR_PREFFIX = "facades/";
	public static final String OBJECTS_TARGET_FOLDER_NAME = "objects";
	public static final String SPECIAL_OBJECTS_TARGET_FOLDER_NAME = "specobjects";
	public static final String FORESTS_TARGET_FOLDER_NAME = "forests";
	private List<String> objectsList = new ArrayList<String>();
	private List<String> singlesFacadesList = new ArrayList<String>();
	private List<String> facadesList = new ArrayList<String>();
	private List<String> forestsList = new ArrayList<String>();
	private List<String> drapedPolysList = new ArrayList<String>();
	private List<String> polygonsList = new ArrayList<String>();
	private List<String> lightsObjectsList = new ArrayList<String>();
	private List<FacadeTagRule> facadesRules = XPlaneOptionsProvider.getOptions().getFacadesRules().getRules();

	private FacadeSetManager facadeSetManager;
	private String targetFolderPath;
	
	private long lastPolyId = -1;
	private int lastFacade = -1;

	/**
	 * @param facadeSet
	 */
	public DsfObjectsProvider(String folderPath, FacadeSetManager facadeSetManager) {
		this.facadeSetManager = facadeSetManager;
		this.targetFolderPath = folderPath;
		computePolygonsList();
		computeObjectsList();
		copyUserResources();
	}

	/**
	 * @param folderPath target folder path
	 */
	public DsfObjectsProvider(String folderPath) {
		this(folderPath, null);
	}

	/**
	 * Return the dsf index of the computed facade file
	 * 
	 * @param simpleBuilding
	 * @param buildingType
	 * @param slopedRoof
	 * @param xVectorLength
	 * @return Integer the facade file index
	 */
	public Integer computeFacadeDsfIndex(Boolean simpleBuilding,
			BuildingType buildingType, Boolean slopedRoof, OsmPolygon osmPolygon) {
		if (osmPolygon.getId() == lastPolyId && lastFacade >= 0) {
			return lastFacade;
		}
		Color roofColor = osmPolygon.getRoofColor();
		Double minVector = osmPolygon.getMinVectorSize();
		Integer height = osmPolygon.getHeight();
		Facade resFacade = null;
		if (slopedRoof) {
			resFacade = facadeSetManager.getRandomHouseSlopedFacade(buildingType, minVector, height, roofColor); 
		}
		if (resFacade == null) {
			resFacade = facadeSetManager.getRandomFacade(buildingType,height,simpleBuilding);
		}
		
		int idx = getFacadeStrIndex(resFacade.getFile());
		lastPolyId = osmPolygon.getId();
		lastFacade = idx;
		return idx;
	}
	
	public int computeFacadeIndexFromRules(OsmPolygon osmPolygon) {
		if (osmPolygon.getId() == lastPolyId && lastFacade >= 0) {
			return lastFacade;
		}
		String name = getFacadeNameFromRules(osmPolygon);
		if (name != null) {
			int idx = getFacadeStrIndex(name);
			lastPolyId = osmPolygon.getId();
			lastFacade = idx;
			return idx;
		}
		return -1;
	}

	/**
	 * Copies custom user resources from {osm2xp}/xplane/reources folder
	 */
	private void copyUserResources() {
		File userResourcesFolder = PathsService.getPathsProvider().getUserResourcesFolder();
		if (userResourcesFolder.isDirectory()) {
			try {
				FilesUtils.copyDirectory(userResourcesFolder, new File(targetFolderPath), true);
			} catch (IOException e) {
				Osm2xpLogger.log(e);
			}
		}
	}

	private int getFacadeStrIndex(String name) {
		if (!name.startsWith(FACADES_DIR_PREFFIX)) {
			return polygonsList.indexOf(FACADES_DIR_PREFFIX + name);
		} else {
			return polygonsList.indexOf(name);
		}
	}
	
	protected String getFacadeNameFromRules(OsmPolygon osmPolygon) {
		for (FacadeTagRule facadeTagRule : facadesRules) {
			if (facadeTagRule.matches(osmPolygon)) {
				List<ObjectFile> objectsFiles = facadeTagRule.getObjectsFiles();
				Random rand = new Random();
				return objectsFiles.get(rand.nextInt(objectsFiles.size())).getPath();
			}
		}
		return null;
	}
	
	public Integer computeSpecialFacadeDsfIndex(SpecialFacadeType specialFacadeType, OsmPolyline polyline) {
		if (polyline.getId() == lastPolyId && lastFacade >= 0) {
			return lastFacade;
		}
		Facade randomSpecialFacade = facadeSetManager.getRandomSpecialFacade(specialFacadeType);
		if (randomSpecialFacade != null) {
			int idx = getFacadeStrIndex(randomSpecialFacade.getFile());
			lastPolyId = polyline.getId();
			lastFacade = idx;
			return idx;
		}
		return -1;
	}


	/**
	 * @param facadeSet
	 * @throws Osm2xpBusinessException
	 */
	public void computePolygonsList() {

		facadesList.clear();
		forestsList.clear();
		drapedPolysList.clear();
		polygonsList.clear();
		singlesFacadesList.clear();

		// FORESTS RULES
		if (XPlaneOptionsProvider.getOptions().isGenerateFor()) {
			
			for (ForestTagRule forest : XPlaneOptionsProvider.getOptions()
					.getForestsRules().getRules()) {
				for (ObjectFile file : forest.getObjectsFiles()) {
					if (!forestsList.contains(file.getPath())) {
						forestsList.add(file.getPath());
					}
					
				}
			}
			copyForestFiles();
			polygonsList.addAll(forestsList);
			
		}
		// DRAPED POLYGONS RULES
		if (XPlaneOptionsProvider.getOptions().isGeneratePolys()) {

			for (PolygonTagsRule forest : XPlaneOptionsProvider.getOptions()
					.getPolygonRules().getRules()) {
				for (Polygon poly : forest.getPolygons()) {
					if (!drapedPolysList.contains(poly.getPath())) {
						drapedPolysList.add(poly.getPath());
					}

				}
			}
			//TODO actual copying polygons
			polygonsList.addAll(drapedPolysList);
			
		}
		// FACADES RULES
		if (!XPlaneOptionsProvider.getOptions().getFacadesRules().getRules()
				.isEmpty()) {
			for (FacadeTagRule facadeTagRule : XPlaneOptionsProvider.getOptions()
					.getFacadesRules().getRules()) {
				for (ObjectFile file : facadeTagRule.getObjectsFiles()) {
					if (!singlesFacadesList.contains(file.getPath())) {
						singlesFacadesList.add(file.getPath());
					}
				}
			}
			polygonsList.addAll(singlesFacadesList);
		}

		// BASIC BUILDINGS FACADES
		if (XPlaneOptionsProvider.getOptions().isGenerateBuildings()) {
			facadesList.addAll(facadeSetManager.getAllFacadeStrings());
			polygonsList.addAll(facadesList.stream().map(str -> FACADES_DIR_PREFFIX + str).collect(Collectors.toList()));
		}

	}

	/**
	 * 
	 */
	public void computeObjectsList() {
		objectsList.clear();
		// add 3D objects
//		for (XplaneObjectTagRule object : XPlaneOptionsProvider.getOptions() TODO Not needed, since we register object during copy operation
//				.getObjectsRules().getRules()) {
//			for (ObjectFile file : object.getObjectsFiles()) {
//				if (!objectsList.contains(file.getPath())) {
//					objectsList.add(file.getPath());
//				}
//
//			}
//		}
		
		//add special 3d objects (e.g. chimneys)
		add3DObjects();

		// add lights objects
		for (XplaneLightTagRule object : XPlaneOptionsProvider.getOptions()
				.getLightsRules().getRules()) {
			for (ObjectFile file : object.getObjectsFiles()) {
				if (!objectsList.contains(file.getPath())) {
					objectsList.add(file.getPath());
				}

			}
		}
	}
	
	private void copyForestFiles() {
		File forestsFolder = PathsService.getPathsProvider().getForestsFolder();
		if (forestsFolder.isDirectory()) {
			try {
				FilesUtils.copyDirectory(forestsFolder, new File(targetFolderPath, FORESTS_TARGET_FOLDER_NAME),
						false);
			} catch (IOException e) {
				Osm2xpLogger.log(e);
			}
		}
	}

	private void add3DObjects() {
		if (XPlaneOptionsProvider.getOptions().isGenerateChimneys() || XPlaneOptionsProvider.getOptions().isGenerateObj() ) {
				File objectsFolder = PathsService.getPathsProvider().getObjectsFolder();
				if (objectsFolder.isDirectory()) {
					registerAndCopyObjectsFolder(objectsFolder, OBJECTS_TARGET_FOLDER_NAME);
				} else {
					Osm2xpLogger.error("Special objects folder not present in resources dir - this can result in generation errors");
				} 
				File specObjectsFolder = PathsService.getPathsProvider().getSpecObjectsFolder();
				if (specObjectsFolder.isDirectory()) {
					registerAndCopyObjectsFolder(specObjectsFolder, SPECIAL_OBJECTS_TARGET_FOLDER_NAME);
				} else {
					Osm2xpLogger.error("Special objects folder not present in resources dir - this can result in generation errors");
				} 
			}
	}

	protected void registerAndCopyObjectsFolder(File objectsFolder, String targetSubfolder) {
		try {
			FilesUtils.copyDirectory(objectsFolder, new File(targetFolderPath, targetSubfolder),
					false);
		} catch (IOException e) {
			Osm2xpLogger.log(e);
		}
		File[] objFiles = objectsFolder.listFiles((parent, name) -> name.toLowerCase().endsWith(".obj"));
		for (File file : objFiles) {
			objectsList.add(targetSubfolder + "/" + file.getName());
		}
	}

	/**
	 * @param tagRule
	 * @return
	 */
	public Integer getRandomObject(TagsRule tagRule) {
		Random rnd = new Random();
		int i = rnd.nextInt(tagRule.getObjectsFiles().size());
		String objectFile = tagRule.getObjectsFiles().get(i).getPath();
		return objectsList.indexOf(objectFile);
	}
	
	/**
	 * @return
	 */
	@Deprecated
	public Integer getRandomStreetLightObject() {
		return null;
	}

	/**
	 * @param forestTagRule
	 * @return
	 */
	public Integer getRandomForest(ForestTagRule forestTagRule) {
		List<ObjectFile> objectsFiles = forestTagRule.getObjectsFiles();
		Random rnd = new Random();
		int i = rnd.nextInt(objectsFiles.size());
		String forestFile = objectsFiles.get(i).getPath();
		return polygonsList.indexOf(forestFile);
	}
	
	public int getStringIndex(String str) {
		return polygonsList.indexOf(str);
	}

	/**
	 * return a random object index and the angle for the first matching rule
	 * 
	 * @param tags
	 * @return
	 */
	public XplaneDsf3DObject getRandomDsfObjectIndexAndAngle(List<Tag> tags,
			Long id) {
		XplaneDsf3DObject result = null;
		for (Tag tag : tags) {
			for (XplaneObjectTagRule objectTagRule : XPlaneOptionsProvider
					.getOptions().getObjectsRules().getRules()) {
				if ((objectTagRule.getTag().getKey().equalsIgnoreCase("id") && objectTagRule
						.getTag().getValue()
						.equalsIgnoreCase(String.valueOf(id)))
						|| (OsmUtils.compareTags(objectTagRule.getTag(), tag))) {
					result = new XplaneDsf3DObject();
					result.setRule(objectTagRule);
					result.setDsfIndex(getRandomObject(objectTagRule));
					if (objectTagRule.isRandomAngle()) {
						Random randomGenerator = new Random();
						result.setAngle(randomGenerator.nextInt(360));
					} else {
						result.setAngle(objectTagRule.getAngle());
					}
					break;
				}
			}
		}
		return result;
	}

	/**
	 * return a random object index and the angle for the first matching rule
	 * 
	 * @param tags
	 * @return
	 */
	public XplaneDsfObject getRandomDsfObject(OsmPolygon osmPolygon) {
		LinearRing2D polygon = osmPolygon.getPolygon();
		XplaneDsfObject result = null;
		// shuffle rules
		List<XplaneObjectTagRule> tagsRules = new ArrayList<XplaneObjectTagRule>();
		tagsRules.addAll(XPlaneOptionsProvider.getOptions().getObjectsRules()
				.getRules());
		Collections.shuffle(tagsRules);
		for (Tag tag : osmPolygon.getTags()) {
			for (XplaneObjectTagRule rule : tagsRules) {
				// check Tag matching
				if ((rule.getTag().getKey().equalsIgnoreCase("id") && rule
						.getTag().getValue()
						.equalsIgnoreCase(String.valueOf(osmPolygon.getId())))
						|| (OsmUtils.compareTags(rule.getTag(), tag))) {
					// check rule options

					Boolean checkArea = !rule.isAreaCheck()
							|| (rule.isAreaCheck() && (osmPolygon.getArea() > rule
									.getMinArea() && osmPolygon.getArea() < rule
									.getMaxArea()));

					Boolean checkSize = !rule.isSizeCheck()
							|| GeomUtils.isRectangleBigEnoughForObject(
									rule.getxVectorMaxLength(),
									rule.getyVectorMaxLength(),
									rule.getxVectorMinLength(),
									rule.getyVectorMinLength(), polygon);

					Boolean checkSimplePoly = !rule.isSimplePolygonOnly()
							|| (rule.isSimplePolygonOnly() && osmPolygon
									.isSimplePolygon());

					if (checkArea && checkSize && checkSimplePoly) {
						result = new XplaneDsf3DObject(osmPolygon, rule);
						// compute object index
						result.setDsfIndex(getRandomObject(rule));

					}
				}
			}
		}
		return result;
	}

	/**
	 * @param tags
	 * @param dsfObjectsProvider
	 * @return
	 */
	public Integer[] getRandomForestIndexAndDensity(List<Tag> tags) {
		for (Tag tag : tags) {
			for (ForestTagRule forestTagRule : XPlaneOptionsProvider.getOptions()
					.getForestsRules().getRules()) {
				if (OsmUtils.compareTags(forestTagRule.getTag(), tag)) {
					Integer[] result = new Integer[2];
					result[0] = getRandomForest(forestTagRule);
					result[1] = forestTagRule.getForestDensity();
					return result;

				}
			}
		}
		return null;
	}

	/**
	 * @return the objectsList
	 */
	public List<String> getObjectsList() {
		return objectsList;
	}

	/**
	 * @param objectsList
	 *            the objectsList to set
	 */
	public void setObjectsList(List<String> objectsList) {
		this.objectsList = objectsList;
	}

	/**
	 * @return the singlesFacadesList
	 */
	public List<String> getSinglesFacadesList() {
		return singlesFacadesList;
	}

	/**
	 * @param singlesFacadesList
	 *            the singlesFacadesList to set
	 */
	public void setSinglesFacadesList(List<String> singlesFacadesList) {
		this.singlesFacadesList = singlesFacadesList;
	}

	/**
	 * @return the facadesList
	 */
	public List<String> getFacadesList() {
		return facadesList;
	}

	/**
	 * @param facadesList
	 *            the facadesList to set
	 */
	public void setFacadesList(List<String> facadesList) {
		this.facadesList = facadesList;
	}

	/**
	 * @return the forestsList
	 */
	public List<String> getForestsList() {
		return forestsList;
	}

	/**
	 * @param forestsList
	 *            the forestsList to set
	 */
	public void setForestsList(List<String> forestsList) {
		this.forestsList = forestsList;
	}

	/**
	 * @return the polygonsList
	 */
	public List<String> getPolygonsList() {
		return Collections.unmodifiableList(polygonsList);
	}

	public XplaneDsfObject getRandomDsfLightObject(OsmPolygon osmPolygon) {
		XplaneDsfObject result = null;
		// shuffle rules
		List<XplaneLightTagRule> tagsRules = new ArrayList<XplaneLightTagRule>();
		tagsRules.addAll(XPlaneOptionsProvider.getOptions().getLightsRules()
				.getRules());
		Collections.shuffle(tagsRules);
		for (Tag tag : osmPolygon.getTags()) {
			for (XplaneLightTagRule rule : tagsRules) {
				// check Tag matching
				if ((rule.getTag().getKey().equalsIgnoreCase("id") && rule
						.getTag().getValue()
						.equalsIgnoreCase(String.valueOf(osmPolygon.getId())))
						|| (OsmUtils.compareTags(rule.getTag(), tag))) {
					// percentage check
					Random rand = new Random();
					int min = 0;
					int max = 100;
					int percentage = rand.nextInt(max - min + 1) + min;
					if (percentage < rule.getPercentage()) {
						result = new XplaneDsfLightObject(osmPolygon, rule);
						// compute object index
						result.setDsfIndex(getRandomObject(rule));
					}
				}
			}
		}
		return result;
	}

	public Integer getSpecialObject(String specialObjectFile) {
		return objectsList.indexOf(SPECIAL_OBJECTS_TARGET_FOLDER_NAME + "/" + specialObjectFile);
	}

}