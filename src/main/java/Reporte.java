public class Reporte {

    private String fecha;
    private String nombre;
    private String edad;
    private String cedula;
    private String ars;
    private String procedimiento;
    private String enzianP;
    private String enzianO;
    private String enzianO2;
    private String enzianT;
    private String enzianT2;
    private String enzianA;
    private String enzianB;
    private String enzianB2;
    private String enzianC;
    private String enzianF;
    //private String enzianF2;
    private String resumenQx;
    private String postQx;

    public Reporte(String fecha, String nombre, String edad,String cedula, String ars, String procedimiento,
                   String enzianP, String enzianO, String enzianT, String enzianA, String enzianB, String enzianC,
                   String enzianF, String resumenQx, String postQx) {

        this.fecha = fecha;
        this.nombre = nombre;
        this.edad = edad;
        this.cedula = cedula;
        this.ars = ars;
        this.procedimiento = procedimiento;
        this.enzianP = enzianP;
        this.enzianO = enzianO;
        this.enzianT = enzianT;
        this.enzianA = enzianA;
        this.enzianB = enzianB;
        this.enzianC = enzianC;
        this.enzianF = enzianF;
        this.resumenQx = resumenQx;
        this.postQx = postQx;

    }

    public Reporte() {}

    public String getFecha() {
        return fecha;
    }
    public void setFecha(String fecha) {this.fecha = fecha;}

    public String getNombre() {return nombre;}
    public void setNombre(String nombre) {this.nombre = nombre;}

    public String getEdad() {return edad;}
    public void setEdad(String edad) {this.edad = edad;}

    public String getCedula() {return cedula;}
    public void setCedula(String cedula) {this.cedula = cedula;}

    public String getArs() {
        return ars;
    }
    public void setArs(String ars) { this.ars = ars;}

    public String getProcedimiento() {return procedimiento;}
    public void setProcedimiento(String procedimiento) {this.procedimiento = procedimiento;}

    public String getEnzianP() {return enzianP;}
    public void setEnzianP(String enzianP) {this.enzianP = enzianP;}
    public String getEnzianO() {return enzianO;}
    public void setEnzianO(String enzianO) {this.enzianO = enzianO;}
    public String getEnzianO2() {return enzianO2;}
    public void setEnzianO2(String enzianO2) {
        this.enzianO2 = enzianO2;
    }
    public String getEnzianT() {return enzianT;}
    public void setEnzianT(String enzianT) {this.enzianT = enzianT;}
    public String getEnzianT2() {return enzianT2;}
    public void setEnzianT2(String enzianT2) {this.enzianT2 = enzianT2;}
    public String getEnzianA() {return enzianA;}
    public void setEnzianA(String enzianA) {this.enzianA = enzianA;}
    public String getEnzianB() {return enzianB;}
    public void setEnzianB(String enzianB) {this.enzianB = enzianB;}
    public String getEnzianB2() {return enzianB2;}
    public void setEnzianB2(String enzianB2) {this.enzianB2 = enzianB2;}
    public String getEnzianC() {return enzianC;}
    public void setEnzianC(String enzianC) {this.enzianC = enzianC;}
    public String getEnzianF() {return enzianF;}
    public void setEnzianF(String enzianF) {this.enzianF = enzianF;}

    public String getResumenQx() {return resumenQx;}
    public void setResumenQx(String resumenQx) {this.resumenQx = resumenQx;}

    public String getPostQx() {return postQx;}
    public void setPostQx(String postQx) {this.postQx = postQx;}

}
