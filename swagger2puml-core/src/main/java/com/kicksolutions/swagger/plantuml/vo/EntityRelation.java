package com.kicksolutions.swagger.plantuml.vo;

/**
 * @author David Gileadi
 *
 */
public class EntityRelation implements Relation {

	private String target;
	private boolean isRequired;
	private boolean isMany;
	private String source;
	private String sourceField;

	public EntityRelation() {
		super();
	}

	public EntityRelation(String target, boolean isRequired, boolean isMany, String source, String sourceField) {
		super();
		this.target = target;
		this.isRequired = isRequired;
		this.isMany = isMany;
		this.source = source;
		this.sourceField = sourceField;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public boolean isRequired() {
		return isRequired;
	}

	public void setRequired(boolean isRequired) {
		this.isRequired = isRequired;
	}

	public boolean isMany() {
		return isMany;
	}

	public void setMany(boolean isMany) {
		this.isMany = isMany;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSourceField() {
		return sourceField;
	}

	public void setSourceField(String sourceField) {
		this.sourceField = sourceField;
	}

	public String getCurly() {
		return "{";
	}

	@Override
	public String toString() {
		return "ClassRelation [target=" + target +
				", isRequired=" + isRequired + ", isMany=" + isMany + ", source=" + source + ", sourceField=" + sourceField + "]";
	}
}
