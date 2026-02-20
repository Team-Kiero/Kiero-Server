package com.kiero.parent.application.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.domain.Child;
import com.kiero.parent.application.dto.ChildInfoResponse;
import com.kiero.parent.application.dto.InviteStatusResponse;
import com.kiero.parent.application.port.in.ParentChildQueryUseCase;
import com.kiero.parent.application.port.in.ParentChildSaveUseCase;
import com.kiero.parent.application.port.out.ParentChildAccessPort;
import com.kiero.parent.application.port.out.ParentChildLoadPort;
import com.kiero.parent.application.port.out.ParentChildSavePort;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.domain.ParentChild;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParentChildService implements ParentChildQueryUseCase, ParentChildSaveUseCase {

	private final ParentChildLoadPort parentChildLoadPort;
	private final ParentChildAccessPort parentChildAccessPort;
	private final ParentChildSavePort parentChildSavePort;

	@Override
	@Transactional(readOnly = true)
	public boolean existsByParentIdAndChildId(Long parentId, Long childId) {
		return parentChildAccessPort.existsByParentIdAndChildId(parentId, childId);
	}

	@Override
	@Transactional(readOnly = true)
	public ParentChild save(ParentChild parentChild) {
		return parentChildSavePort.save(parentChild);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Parent> findParentsByChildId(Long childId) {
		return parentChildLoadPort.findParentsByChildId(childId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ChildInfoResponse> getMyChildren(Long parentId) {
		List<ParentChild> parentChildren = parentChildLoadPort.findAllByParentId(parentId);

		return parentChildren.stream()
			.map(pc -> ChildInfoResponse.of(pc.getChild()))
			.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public InviteStatusResponse checkInviteStatus(Long parentId, String childLastName, String childFirstName) {
		List<ParentChild> parentChildren = parentChildLoadPort.findAllByParentId(parentId);

		Optional<Child> matchedChild = parentChildren.stream()
			.map(ParentChild::getChild)
			.filter(child -> child.getLastName().trim().equals(childLastName.trim()) &&
				child.getFirstName().trim().equals(childFirstName.trim()))
			.findFirst();

		if (matchedChild.isPresent()) {
			Child child = matchedChild.get();
			log.info("Child found: parentId={}, childId={}, name={} {}",
				parentId, child.getId(), childLastName, childFirstName);
			return InviteStatusResponse.registered(child.getId());
		} else {
			log.info("Child not found: parentId={}, name={} {}",
				parentId, childLastName, childFirstName);
			return InviteStatusResponse.notRegistered();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<Long> getMyChildIds(Long parentId) {
		return parentChildLoadPort.findChildIdsByParentId(parentId);
	}
}
