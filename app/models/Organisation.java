package models;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class Organisation extends GeneratedIdentityModel {

    private String code;

	private String name;

	private String nameAbbreviation;

    private List<Organisation> children;

	private Organisation parent;

    public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNameAbbreviation() {
		return nameAbbreviation;
	}

	public void setNameAbbreviation(String nameAbbreviation) {
		this.nameAbbreviation = nameAbbreviation;
	}

	public List<Organisation> getChildren() {
		return children;
	}

	@OneToMany(mappedBy = "parent")
	@JsonBackReference
	public void setChildren(List<Organisation> children) {
		this.children = children;
	}

	@ManyToOne(cascade = CascadeType.PERSIST)
	public Organisation getParent() {
		return parent;
	}

	public void setParent(Organisation parent) {
		this.parent = parent;
	}

	@Override
    public String toString() {
        return "Organisation{" +
                "name='" + name + '\'' +
                ", nameAbbreviation='" + nameAbbreviation + '\'' +
                ", code='" + code + '}';
    }
}
