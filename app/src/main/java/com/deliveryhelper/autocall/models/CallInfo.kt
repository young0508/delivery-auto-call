package com.deliveryhelper.autocall.models

data class CallInfo(
    val callCount: Int,        // 콜 개수 (1건, 2건, 3건)
    val totalPrice: Int,       // 총 금액 (원)
    val distance: Double,      // 거리 (km)
    val address: String        // 배달 주소
) {
    override fun toString(): String {
        return "CallInfo(callCount=$callCount, totalPrice=${totalPrice}원, distance=${distance}km, address='$address')"
    }
    
    // 콜 정보가 유효한지 확인
    fun isValid(): Boolean {
        return callCount in 1..3 && 
               totalPrice > 0 && 
               distance > 0 && 
               address.isNotBlank()
    }
    
    // 단건당 평균 금액 계산
    fun getAveragePrice(): Int {
        return totalPrice / callCount
    }
    
    // 시간당 예상 수익 계산 (배달 시간 추정: 거리 * 10분 + 10분 기본)
    fun getEstimatedHourlyIncome(): Int {
        val estimatedTimeMinutes = (distance * 10) + 10
        val hourlyIncome = (totalPrice / estimatedTimeMinutes) * 60
        return hourlyIncome.toInt()
    }
}