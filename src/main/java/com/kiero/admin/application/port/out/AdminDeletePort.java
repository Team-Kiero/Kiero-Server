package com.kiero.admin.application.port.out;

public interface AdminDeletePort {

	// 아이 삭제 시 연관 데이터 cascade 삭제
	void deleteAllSchedulesByChildId(Long childId);
	void deleteAllMissionsByChildId(Long childId);
	void deleteAllCouponsByChildId(Long childId);
	void deleteAllFeedItemsByChildId(Long childId);
	void deleteAllParentChildRelationsByChildId(Long childId);
	void deleteChild(Long childId);

	// 부모 삭제 시 연관 데이터 cascade 삭제
	void deleteAllSchedulesByParentId(Long parentId);
	void deleteAllMissionsByParentId(Long parentId);
	void deleteAllCouponsByParentId(Long parentId);
	void deleteAllFeedItemsByParentId(Long parentId);
	void deleteAllParentChildRelationsByParentId(Long parentId);
	void deleteParent(Long parentId);

	// 단건 삭제
	void deleteScheduleById(Long scheduleId);
	void deleteMissionById(Long missionId);
	void deleteCouponById(Long couponId);
}
