package raft.kvstore.client;

import raft.core.service.NoAvailableServerException;

public class KVStoreGetCommand implements Command{
    @Override
    public String getName() {
        return "get";
    }

    @Override
    public void execute(String arguments, CommandContext context) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("usage: " + getName() + " <key>");
        }

        byte[] valueBytes;
        try {
            valueBytes = context.getClient().get(arguments);
        } catch (NoAvailableServerException e) {
            System.err.println(e.getMessage());
            return;
        }

        if (valueBytes == null) {
            System.out.println("null");
        } else {
            System.out.println(new String(valueBytes));
        }
    }
}
