package uk.ac.bath.masmusic.common;

/**
 * A note onset event.
 *
 * @author Javier Dehesa
 */
public class Onset {

    /** Onset timestamp (ms) */
    private long timestamp;
    /** Onset salience vaule */
    private double salience;

    /**
     * Constructor.
     *
     * @param timestamp
     *            Onset timestamp in milliseconds
     * @param salience
     *            Onset salience value
     */
    public Onset(long timestamp, double salience) {
	this.timestamp = timestamp;
	this.salience = salience;
    }

    /**
     * @return Onset timestamp in milliseconds
     */
    public long getTimestamp() {
	return timestamp;
    }

    /**
     * @return Onset salience
     */
    public double getSalience() {
	return salience;
    }

    @Override
    public String toString() {
	return "Onset [timestamp=" + timestamp + ", salience=" + salience + "]";
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	long temp;
	temp = Double.doubleToLongBits(salience);
	result = prime * result + (int) (temp ^ (temp >>> 32));
	result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	Onset other = (Onset) obj;
	if (Double.doubleToLongBits(salience) != Double.doubleToLongBits(other.salience)) {
	    return false;
	}
	if (timestamp != other.timestamp) {
	    return false;
	}
	return true;
    }
}
