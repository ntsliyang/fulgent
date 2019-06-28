package DataJson.LaneJson.LaneInfos;

public class Accession {
    private String id;
    private long numberOfReads;
    private double lanePercentage;
    private String frontBarcode;
    private String endBarcode;
    private double q30;
    private String status;
    private String message;
    // private String color;

    public Accession(String id, long numberOfReads, double lanePercentage, String frontBarcode, String endBarcode, double q30, String status, String message) {
        this.id = id;
        this.numberOfReads = numberOfReads;
        this.lanePercentage = lanePercentage;
        this.frontBarcode = frontBarcode;
        this.endBarcode = endBarcode;
        this.q30 = q30;
        this.status = status;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public long getNumberOfReads() {
        return numberOfReads;
    }

    public double getLanePercentage() {
        return lanePercentage;
    }

    public String getFrontBarcode() {
        return frontBarcode;
    }

    public String getEndBarcode() {
        return endBarcode;
    }

    public double getQ30() {
        return q30;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
