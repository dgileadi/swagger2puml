/**
 *
 */
package com.kicksolutions.swagger.plantuml.vo;

/**
 * @author MSANTOSH
 *
 */
public class EntityField implements Member {

	private String dataType;
	private String name;
	private String entityName;
	private boolean isRequired;

	public EntityField() {
		super();
	}

	public EntityField(String dataType, String name, String entityName, boolean isRequired) {
		super();
		this.dataType = dataType;
		this.name = name;
		this.entityName = entityName;
		this.isRequired = isRequired;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getClassName() {
		return entityName;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public boolean isRequired() {
		return isRequired;
	}

	public void setRequired(boolean isRequired) {
		this.isRequired = isRequired;
	}

	@Override
	public String toString() {
		return "ClassMembers [dataType=" + dataType + ", name=" + name + ", entityName=" + entityName + ", isRequired="
				+ isRequired + "]";
	}
}
