package fr.emn.elastuff.graph;

import fr.emn.elastuff.utils.Color;
import fr.emn.elastuff.utils.SysOutLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VM extends CloudResource {

	private PM pm; //physical parent
	private Tier tier; //logical parent
	//VM technicals details
	int vcpuConsumption;
	int ramConsumption;
	int diskConsumption;

	public VM(String name) {
		super(name);
        this.cos.initResourceList();
	}

	public int getVcpuConsumption() {
		return vcpuConsumption;
	}

	public void setVcpuConsumption(int vcpuConsumption) {
		this.vcpuConsumption = vcpuConsumption;
		this.setChanged();
		this.notifyObservers(this);
	}

	public int getRamConsumption() {
		return ramConsumption;
	}

	public void setRamConsumption(int ramConsumption) {
		this.ramConsumption = ramConsumption;
		this.setChanged();
		this.notifyObservers(this);
	}

	public int getDiskConsuption() {
		return diskConsumption;
	}

	public void setDiskConsumption(int disk_consumption) {
		this.diskConsumption = disk_consumption;
		this.setChanged();
		this.notifyObservers(this);
	}

	public PM getPm() {
		return (pm);
	}

	public  void setPm(PM p) {
		this.pm = p;
	}

	public Tier getTier() {
		return (tier);
	}

	public void setTier(Tier t) {
		this.tier = t;
	}

    //--CO---------------------------------------------------------------------
    //An VM hold multiple Co Cloud Resources
    private CloudResourceHolder<Co> cos = new CloudResourceHolder<Co>() {

        private List<Co> cos;

        @Override
        public List<Co> getResourceList() {
            return cos;
        }

        @Override
        public void initResourceList() {
            cos = new ArrayList<>();
        }
    };
    public boolean addCo(Co co) { return this.cos.addResource(co); }
    public Optional<Co> getCo(String coName) { return this.cos.getResource(coName); }
    public Optional<Co> getCo(int coId) { return this.cos.getResource(coId); }
    public boolean isCoExists(String coName) { return this.cos.isResourceExists(coName); }
    public int getCoNumber() { return this.cos.getResourceNumber(); }
    //--Co---------------------------------------------------------------------

    @Override
    public void display(int indent) {
        String indentL = this.getIndent(indent);
        SysOutLogger.log(indentL + "[[");
        SysOutLogger.log("VM", Color.CYAN);
        SysOutLogger.log("]{");
        SysOutLogger.log("cpu", Color.BLUE);
        SysOutLogger.log(":");
        SysOutLogger.log(String.valueOf(vcpuConsumption), Color.MAGENTA);
        SysOutLogger.log(", ");
        SysOutLogger.log("ram", Color.BLUE);
        SysOutLogger.log(":");
        SysOutLogger.log(String.valueOf(ramConsumption), Color.MAGENTA);
        SysOutLogger.log(", ");
        SysOutLogger.log("disk", Color.BLUE);
        SysOutLogger.log(":");
        SysOutLogger.log(String.valueOf(diskConsumption), Color.MAGENTA);
        SysOutLogger.log("}{");
        SysOutLogger.log("name", Color.BLUE);
        SysOutLogger.log(":");
        SysOutLogger.log("\"" + name + "\"", Color.CYAN);
        SysOutLogger.log("}{");
        SysOutLogger.log("parent PM", Color.BLUE);
        SysOutLogger.log(":");
        if(this.getPm() == null) {
            SysOutLogger.log("NO PM", Color.RED);
        } else {
            SysOutLogger.log("\"" + pm.getName() + "\"", Color.CYAN);
        }
        SysOutLogger.log(", ");
        SysOutLogger.log("parent Tier", Color.BLUE);
        SysOutLogger.log(":");
        if(this.getTier() == null) {
            SysOutLogger.log("NO TIER", Color.RED);
        } else {
            SysOutLogger.log("\"" + tier.getName() + "\"", Color.CYAN);
        }
        SysOutLogger.log("}]\n");
        for(int i = 0; i < this.getCoNumber(); i++) {
            this.getCo(i).get().display(indent + 1);
        }
    }

}