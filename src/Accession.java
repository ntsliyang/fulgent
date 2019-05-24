public class Accession {
    private String id;
    private long numberOfReads;
    private double lanePercentage;
    private String frontBarcode;
    private String endBarcode;
    private double q30;
    private String status;
    private String message;
    private String color;

    public Accession(String id, long numberOfReads, double lanePercentage, String frontBarcode, String endBarcode, double q30, String status, String message, String color) {
        this.id = id;
        this.numberOfReads = numberOfReads;
        this.lanePercentage = lanePercentage;
        this.frontBarcode = frontBarcode;
        this.endBarcode = endBarcode;
        this.q30 = q30;
        this.status = status;
        this.message = message;
        this.color = color;
    }
}
