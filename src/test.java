public class test {
    public static void main(String[] args) {
        String barcode = "ACGT_EXCLUDED";
        System.out.println(!barcode.contains("EXCLUDED"));
    }
}
