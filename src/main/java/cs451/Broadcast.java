package cs451;

public interface Broadcast {
	public void broadcast(String m);
	public void deliver(int from, String m);
	public void registerBroadcast(Broadcast upper);
}
