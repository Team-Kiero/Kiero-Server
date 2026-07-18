package com.kiero.invitation.application.port.out;

import java.util.concurrent.Callable;

public interface InviteLockPort {
	<T> T withInviteCodeLock(String code, Callable<T> action);

}
