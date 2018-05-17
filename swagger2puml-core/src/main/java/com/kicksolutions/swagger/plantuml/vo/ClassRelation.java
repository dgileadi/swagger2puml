/**
 *
 */
package com.kicksolutions.swagger.plantuml.vo;

/**
 * @author MSANTOSH
 *
 */
public class ClassRelation implements Relation {

	private String target;
	private boolean isExtension;
	private boolean isComposition;
	private String cardinality;
	private String source;

	public ClassRelation() {
		super();
	}

	public ClassRelation(String target, boolean isExtension, boolean isComposition,String cardinality,String source) {
		super();
		this.target = target;
		this.isExtension = isExtension;
		this.isComposition = isComposition;
		this.cardinality = cardinality;
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public boolean isExtension() {
		return isExtension;
	}

	public void setExtension(boolean isExtension) {
		this.isExtension = isExtension;
	}

	public boolean isComposition() {
		return isComposition;
	}

	public void setComposition(boolean isComposition) {
		this.isComposition = isComposition;
	}

	public String getCardinality() {
		return cardinality;
	}

	public void setCardinality(String cardinality) {
		this.cardinality = cardinality;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	@Override
	public String toString() {
		return "ClassRelation [target=" + target + ", isExtension=" + isExtension + ", isComposition="
				+ isComposition + ", cardinality=" + cardinality + ", source=" + source + "]";
	}
}
