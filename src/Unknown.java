public class Unknown {
    private long numberOfReads;
    private String frontBarcode;
    private String endBarcode;
    private String status;
    private String message;
    private String color;

    public Unknown(long numberOfReads, String frontBarcode, String endBarcode, String status, String message, String color) {
        this.numberOfReads = numberOfReads;
        this.frontBarcode = frontBarcode;
        this.endBarcode = endBarcode;
        this.status = status;
        this.message = message;
        this.color = color;
    }

    public long getNumberOfReads() {
        return numberOfReads;
    }

    public void setNumberOfReads(long numberOfReads) {
        this.numberOfReads = numberOfReads;
    }

    public String getFrontBarcode() {
        return frontBarcode;
    }

    public void setFrontBarcode(String frontBarcode) {
        this.frontBarcode = frontBarcode;
    }

    public String getEndBarcode() {
        return endBarcode;
    }

    public void setEndBarcode(String endBarcode) {
        this.endBarcode = endBarcode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
