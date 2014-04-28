package org.jocean.j2se.jvmti;

/**
 * @author isdom
 *
 */
public class LocalVirtualMachineVO {
//	"vmid",
//	"name",
//  "commandLine",
//  "mainClass",
//	"mainArgs",
//	"jvmArgs",
//	"jvmFlags",
//	"vmVersion",
//	"userName",
//	"workdir",
//	"kernelVM",
//	"attachable",
//	"manageable",
//	"connectorAddress"
	private int		vmid;
	private String	name;
	private String 	commandLine;
	private String 	mainClass;
	private String 	mainArgs;
	private String 	jvmArgs;
	private String 	jvmFlags;
	private String 	vmVersion;
	private String	userName;
	private String	workdir;
	private boolean kernelVM;
	private boolean attachable;
	private boolean manageable;
	private String 	connectorAddress;
    private	int	parentPid;
    private	int	ruid;
    private	int	euid;
    private	int	suid;
    private	int	fsuid;
    private	int	rgid;
    private	int	egid;
    private	int	sgid;
    private	int	fsgid;
    
    //	JVM's start time in ms
	private	long	startTime = -1;

	//	JVM's end time in ms
	private	long	endTime = -1;
	
	//	JVM's jmx url act as relay protocol
	private	String	jmxurl;
	
	/**
	 * @return the vmid
	 */
	public int getVmid() {
		return vmid;
	}
	/**
	 * @param vmid the vmid to set
	 */
	public void setVmid(int vmid) {
		this.vmid = vmid;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the commandLine
	 */
	public String getCommandLine() {
		return commandLine;
	}
	/**
	 * @param commandLine the commandLine to set
	 */
	public void setCommandLine(String commandLine) {
		this.commandLine = commandLine;
	}
	/**
	 * @return the mainClass
	 */
	public String getMainClass() {
		return mainClass;
	}
	/**
	 * @param mainClass the mainClass to set
	 */
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}
	/**
	 * @return the mainArgs
	 */
	public String getMainArgs() {
		return mainArgs;
	}
	/**
	 * @param mainArgs the mainArgs to set
	 */
	public void setMainArgs(String mainArgs) {
		this.mainArgs = mainArgs;
	}
	/**
	 * @return the jvmArgs
	 */
	public String getJvmArgs() {
		return jvmArgs;
	}
	/**
	 * @param jvmArgs the jvmArgs to set
	 */
	public void setJvmArgs(String jvmArgs) {
		this.jvmArgs = jvmArgs;
	}
	/**
	 * @return the jvmFlags
	 */
	public String getJvmFlags() {
		return jvmFlags;
	}
	/**
	 * @param jvmFlags the jvmFlags to set
	 */
	public void setJvmFlags(String jvmFlags) {
		this.jvmFlags = jvmFlags;
	}
	/**
	 * @return the vmVersion
	 */
	public String getVmVersion() {
		return vmVersion;
	}
	/**
	 * @param vmVersion the vmVersion to set
	 */
	public void setVmVersion(String vmVersion) {
		this.vmVersion = vmVersion;
	}
	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
	/**
	 * @return the workdir
	 */
	public String getWorkdir() {
		return workdir;
	}
	/**
	 * @param workdir the workdir to set
	 */
	public void setWorkdir(String workdir) {
		this.workdir = workdir;
	}
	/**
	 * @return the kernelVM
	 */
	public boolean isKernelVM() {
		return kernelVM;
	}
	/**
	 * @param kernelVM the kernelVM to set
	 */
	public void setKernelVM(boolean isKernelVM) {
		this.kernelVM = isKernelVM;
	}
	/**
	 * @return the attachable
	 */
	public boolean isAttachable() {
		return attachable;
	}
	/**
	 * @param attachable the attachable to set
	 */
	public void setAttachable(boolean attachable) {
		this.attachable = attachable;
	}
	/**
	 * @return the manageable
	 */
	public boolean isManageable() {
		return manageable;
	}
	/**
	 * @param manageable the manageable to set
	 */
	public void setManageable(boolean manageable) {
		this.manageable = manageable;
	}
	/**
	 * @return the connectorAddress
	 */
	public String getConnectorAddress() {
		return connectorAddress;
	}
	/**
	 * @param connectorAddress the connectorAddress to set
	 */
	public void setConnectorAddress(String connectorAddress) {
		this.connectorAddress = connectorAddress;
	}
	/**
	 * @return the parentPid
	 */
	public int getParentPid() {
		return parentPid;
	}
	/**
	 * @param parentPid the parentPid to set
	 */
	public void setParentPid(int parentPid) {
		this.parentPid = parentPid;
	}
	/**
	 * @return the ruid
	 */
	public int getRuid() {
		return ruid;
	}
	/**
	 * @param ruid the ruid to set
	 */
	public void setRuid(int ruid) {
		this.ruid = ruid;
	}
	/**
	 * @return the euid
	 */
	public int getEuid() {
		return euid;
	}
	/**
	 * @param euid the euid to set
	 */
	public void setEuid(int euid) {
		this.euid = euid;
	}
	/**
	 * @return the suid
	 */
	public int getSuid() {
		return suid;
	}
	/**
	 * @param suid the suid to set
	 */
	public void setSuid(int suid) {
		this.suid = suid;
	}
	/**
	 * @return the fsuid
	 */
	public int getFsuid() {
		return fsuid;
	}
	/**
	 * @param fsuid the fsuid to set
	 */
	public void setFsuid(int fsuid) {
		this.fsuid = fsuid;
	}
	/**
	 * @return the rgid
	 */
	public int getRgid() {
		return rgid;
	}
	/**
	 * @param rgid the rgid to set
	 */
	public void setRgid(int rgid) {
		this.rgid = rgid;
	}
	/**
	 * @return the egid
	 */
	public int getEgid() {
		return egid;
	}
	/**
	 * @param egid the egid to set
	 */
	public void setEgid(int egid) {
		this.egid = egid;
	}
	/**
	 * @return the sgid
	 */
	public int getSgid() {
		return sgid;
	}
	/**
	 * @param sgid the sgid to set
	 */
	public void setSgid(int sgid) {
		this.sgid = sgid;
	}
	/**
	 * @return the fsgid
	 */
	public int getFsgid() {
		return fsgid;
	}
	/**
	 * @param fsgid the fsgid to set
	 */
	public void setFsgid(int fsgid) {
		this.fsgid = fsgid;
	}
	
	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}
	/**
	 * @param startTime the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return endTime;
	}
	/**
	 * @param endTime the endTime to set
	 */
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	/**
	 * @return the jmxurl
	 */
	public String getJmxurl() {
		return jmxurl;
	}
	/**
	 * @param jmxurl the jmxurl to set
	 */
	public void setJmxurl(String jmxurl) {
		this.jmxurl = jmxurl;
	}
}
