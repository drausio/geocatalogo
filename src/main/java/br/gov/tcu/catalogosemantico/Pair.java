package br.gov.tcu.catalogosemantico;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Pair {
	
	    protected String first;
	    protected String second;

	    public Pair() {
	    }

	    public String getFirst() {
	        return first;
	    }

	    public void setFirst(String first) {
	        this.first = first;
	    }

	    public String getSecond() {
	        return second;
	    }

	    public void setSecond(String second) {
	        this.second = second;
	    }
}
