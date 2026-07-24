
const locationReady = computed(() => punchLocation.value.status === 'success')
const locationTagType = computed(() => {
  switch (punchLocation.value.status) {
    case 'success': return 'success'
    case 'loading': return 'warning'
    case 'error': return 'danger'
    default: return 'info'
  }
})
const locationTagText = computed(() => {
  switch (punchLocation.value.status) {
    case 'success': return '已定位'
    case 'loading': return '定位中'
    case 'error': return '定位失败'
    default: return '未定位'
  }
})

// 计算统计
const normalRate = computed(() => {
  if (!records.value.length) return 100
  const normalCount = records.value.filter(r => r.status === 'NORMAL' || r.status === 'RECHECKED').length
  return Math.round((normalCount / records.value.length) * 100)
})

const lateOrEarlyCount = computed(() => {
  return records.value.filter(r => r.status === 'LATE' || r.status === 'EARLY_LEAVE').length
})

// 接口方法
const fetchTodayStatus = async () => {
  try {
    const res = await getTodayStatusApi()
    if (res && res.data) {
      todayStatus.value = res.data
    }
  } catch (err) {
    console.error('获取今日打卡状态失败:', err)
  }
}

const fetchMyRecords = async () => {
  tableLoading.value = true
  try {
    const params = {
      page: pagination.value.page,
      pageSize: pagination.value.pageSize,
      startDate: dateRange.value?.[0] || '',
      endDate: dateRange.value?.[1] || ''
    }
    const res = await getMyRecordsApi(params)
    if (res && res.data) {
      records.value = res.data.list || []
      pagination.value.total = res.data.total || 0
    }
  } catch (err) {
    console.error('获取考勤记录失败:', err)
  } finally {
    tableLoading.value = false
  }
}

const fetchDeptOverview = async () => {
  deptLoading.value = true
  try {
    const params = {
      date: overviewDate.value,
      deptId: overviewDeptId.value || null
    }
    const res = await getDeptTodayOverviewApi(params)
    if (res && res.data) {
      deptOverview.value = res.data
    }
  } catch (err) {
    console.error('获取考勤监控失败:', err)
  } finally {
    deptLoading.value = false
  }
}

const fetchDeptTree = async () => {
  if (!isAdminOrHr.value) return
  try {
    const res = await getDeptTreeApi()
    if (res && res.data) {
      deptList.value = res.data
    }
  } catch (error) {
    console.error('Failed to fetch dept tree', error)
  }
}

const fetchRules = async () => {
  ruleLoading.value = true
  try {
    const res = await getRulesApi()
    if (res && res.data) {
      ruleList.value = res.data
    }
  } catch (err) {
    console.error('获取考勤规则失败:', err)
  } finally {
    ruleLoading.value = false
  }
}

const fetchGroups = async () => {
  groupLoading.value = true
  try {
    const res = await getGroupsApi()
    if (res && res.data) {
      groupList.value = res.data
    }
  } catch (err) {
    console.error('获取考勤组失败:', err)
  } finally {
    groupLoading.value = false
  }
}

const loadActiveTabData = (val) => {
  if (val === 'dept') {
    fetchDeptTree()
    fetchDeptOverview()
  } else if (val === 'rule') {
    fetchRules()
    fetchGroups()
  } else {
    fetchTodayStatus()
    fetchMyRecords()
  }
}

watch(activeTab, loadActiveTabData)

const openCheckDialog = (type) => {
  dialogType.value = type
  remark.value = ''
  resetPunchLocation()
  dialogVisible.value = true
  fetchLocationConfig()
  requestPunchLocation()
}

const submitCheck = async () => {
  if (!locationReady.value) {
    ElMessage.warning('请先完成定位后再打卡')
    return
  }
  loading.value = true
  try {
    const data = {
      remark: remark.value,
      latitude: punchLocation.value.latitude,
      longitude: punchLocation.value.longitude,
      accuracyMeters: punchLocation.value.accuracyMeters
    }
    if (dialogType.value === 'checkIn') {
      await checkInApi(data)
      ElMessage.success('上班打卡成功！')
    } else {
      await checkOutApi(data)
      ElMessage.success('下班打卡成功！')
    }
    dialogVisible.value = false
    await fetchTodayStatus()
    await fetchMyRecords()
  } catch (err) {
    // 拦截器处理
  } finally {
    loading.value = false
  }
}

const resetPunchLocation = () => {
  punchLocation.value = {
    status: 'idle',
    title: '等待定位',
    message: '正在准备获取当前位置。',
    latitude: null,
    longitude: null,
    accuracyMeters: null
  }
}

const requestPunchLocation = () => {
  if (!navigator.geolocation) {
    punchLocation.value = {
      status: 'error',
      title: '浏览器不支持定位',
      message: '请使用支持 Geolocation 的浏览器，或联系管理员处理。',
      latitude: null,
      longitude: null,
      accuracyMeters: null
    }
    return
  }

  locating.value = true
  punchLocation.value = {
    status: 'loading',
    title: '正在获取定位',
    message: '请在浏览器弹窗中允许 OfficeFlow 获取当前位置。',
    latitude: null,
    longitude: null,
    accuracyMeters: null
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      updatePunchLocation(position.coords)
      locating.value = false
    },
    (error) => {
      punchLocation.value = {
        status: 'error',
        title: '定位失败',
        message: getLocationErrorMessage(error),
        latitude: null,
        longitude: null,
        accuracyMeters: null
      }
      locating.value = false
    },
    {
      enableHighAccuracy: true,
      timeout: 12000,
      maximumAge: 0
    }
  )
}

const fetchLocationConfig = async () => {
  try {
    const res = await getLocationConfigApi()
    if (res?.data) {
      locationConfig.value = res.data
      if (punchLocation.value.status === 'success') {
        updatePunchLocation({
          latitude: punchLocation.value.latitude,
          longitude: punchLocation.value.longitude,
          accuracy: punchLocation.value.accuracyMeters
        })
      }
    }
  } catch (error) {
    console.error('获取定位打卡配置失败', error)
  }
}

const updatePunchLocation = (coords) => {
  const latitude = coords.latitude
  const longitude = coords.longitude
  const accuracy = coords.accuracy
  const config = locationConfig.value
  let status = 'success'
  let title = '定位已获取'
  let message = '提交后后端会按考勤规则校验是否在允许打卡范围内。'

  if (config.locationRequired && !config.locationConfigured) {
    status = 'error'
    title = '办公点未配置'
    message = '当前考勤规则要求定位打卡，但管理员还没有维护办公点坐标。'
  } else if (config.locationConfigured) {
    const distance = calcDistanceMeters(latitude, longitude, Number(config.officeLatitude), Number(config.officeLongitude))
    if (accuracy && accuracy > Number(config.accuracyThresholdMeters || 1000)) {
      status = 'error'
      title = '定位精度不足'
      message = `当前定位精度约 ${Math.round(accuracy)} 米，超过允许精度 ${config.accuracyThresholdMeters || 1000} 米。`
    } else if (config.locationRequired && distance > Number(config.allowedRadiusMeters || 1000)) {
      status = 'error'
      title = '超出打卡范围'
      message = `当前位置距离 ${config.officeLocationName || '办公地点'} 约 ${distance} 米，超过允许范围 ${config.allowedRadiusMeters || 1000} 米。`
    } else {
      message = `当前位置距离 ${config.officeLocationName || '办公地点'} 约 ${distance} 米，允许范围 ${config.allowedRadiusMeters || 1000} 米。`
    }
  } else if (!config.locationRequired) {
    message = '当前规则未强制配置办公点，系统会记录本次定位信息。'
  }

  punchLocation.value = {
    status,
    title,
    message,
    latitude,
    longitude,
    accuracyMeters: accuracy
  }
}

const calcDistanceMeters = (lat1, lon1, lat2, lon2) => {
  const radius = 6371008.8
  const radLat1 = lat1 * Math.PI / 180
  const radLat2 = lat2 * Math.PI / 180
  const deltaLat = (lat2 - lat1) * Math.PI / 180
  const deltaLon = (lon2 - lon1) * Math.PI / 180
  const a = Math.sin(deltaLat / 2) ** 2 + Math.cos(radLat1) * Math.cos(radLat2) * Math.sin(deltaLon / 2) ** 2
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return Math.round(radius * c)
}

const getCurrentPositionOnce = () => {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('浏览器不支持定位'))
      return
    }
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 12000,
      maximumAge: 0
    })
  })
}

const fillOfficeLocationFromCurrentPosition = async () => {
  officeLocating.value = true
  try {
    const position = await getCurrentPositionOnce()
    ruleForm.value.officeLatitude = Number(position.coords.latitude.toFixed(7))
    ruleForm.value.officeLongitude = Number(position.coords.longitude.toFixed(7))
    if (!ruleForm.value.officeLocationName) {
      ruleForm.value.officeLocationName = 'OfficeFlow 办公点'
    }
    ElMessage.success('已填入当前位置作为办公点坐标')
  } catch (error) {
    ElMessage.error(getLocationErrorMessage(error))
  } finally {
    officeLocating.value = false
  }
}

const getLocationErrorMessage = (error) => {
  if (error?.code === 1) return '你拒绝了浏览器定位授权，请在浏览器地址栏权限设置中允许定位。'
  if (error?.code === 2) return '暂时无法获取定位，请检查网络、Wi-Fi 或系统定位服务。'
  if (error?.code === 3) return '定位超时，请移动到网络较好的位置后重试。'
  return error?.message || '获取定位失败，请稍后重试。'
}

// 规则编辑与保存
const openRuleDialog = (row) => {
  if (row) {
    ruleForm.value = {
      id: row.id,
      ruleName: row.ruleName,
      workStartTime: row.workStartTime,
      workEndTime: row.workEndTime,
      lateThresholdMinutes: row.lateThresholdMinutes,
      earlyLeaveThresholdMinutes: row.earlyLeaveThresholdMinutes,
      absentThresholdMinutes: row.absentThresholdMinutes,
      locationRequired: !!row.locationRequired,
      officeLocationName: row.officeLocationName || '',
      officeAddress: row.officeAddress || '',
      officeLatitude: row.officeLatitude ?? null,
      officeLongitude: row.officeLongitude ?? null,
      allowedRadiusMeters: row.allowedRadiusMeters || 1000,
      accuracyThresholdMeters: row.accuracyThresholdMeters || 1000
    }
  } else {
    ruleForm.value = {
      id: null,
      ruleName: '新考勤班次规则',
      workStartTime: '09:00:00',
      workEndTime: '18:00:00',
      lateThresholdMinutes: 10,
      earlyLeaveThresholdMinutes: 10,
      absentThresholdMinutes: 240,
      locationRequired: true,
      officeLocationName: 'OfficeFlow 办公点',
      officeAddress: '',
      officeLatitude: null,
      officeLongitude: null,
      allowedRadiusMeters: 1000,
      accuracyThresholdMeters: 1000
    }
  }
  ruleDialogVisible.value = true
}

const saveRule = async () => {
  ruleSubmitting.value = true
  try {
    if (ruleForm.value.id) {
      await updateRuleApi(ruleForm.value.id, ruleForm.value)
      ElMessage.success('修改考勤规则成功！')
    } else {
      await createRuleApi(ruleForm.value)
      ElMessage.success('新建考勤规则成功！')
    }
    ruleDialogVisible.value = false
    await fetchRules()
  } catch (err) {
    // 拦截器处理
  } finally {
    ruleSubmitting.value = false
  }
}

// 考勤组编辑与保存
const openGroupDialog = (row) => {
  if (row) {
    groupForm.value = {
      id: row.id,
      groupName: row.groupName,
      ruleId: row.ruleId,
      deptId: row.deptId
    }
  } else {
    groupForm.value = {
      id: null,
      groupName: '新部门考勤组',
      ruleId: ruleList.value[0]?.id || 1,
      deptId: 2
    }
  }
  groupDialogVisible.value = true
}

const saveGroup = async () => {
  groupSubmitting.value = true
  try {
    if (groupForm.value.id) {
      await updateGroupApi(groupForm.value.id, groupForm.value)
      ElMessage.success('修改部门考勤组绑定成功！')
    } else {
      await createGroupApi(groupForm.value)
      ElMessage.success('新建部门考勤组绑定成功！')
    }
    groupDialogVisible.value = false
    await fetchGroups()
  } catch (err) {
    // 拦截器处理
  } finally {
    groupSubmitting.value = false
  }
}

// 转换助手
const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  return new Date(timeStr).toTimeString().split(' ')[0]
}

const formatMinutes = (mins) => {
  if (!mins || mins <= 0) return '0分钟'
  const h = Math.floor(mins / 60)
  const m = mins % 60
  if (h > 0 && m > 0) return `${h}小时${m}分钟`
  if (h > 0) return `${h}小时`
  return `${m}分钟`
}

const formatRecordLocation = (row) => {
  const inText = formatOneLocation('上班', row.checkInLocationName, row.checkInDistanceMeters)
  const outText = formatOneLocation('下班', row.checkOutLocationName, row.checkOutDistanceMeters)
  return [inText, outText].filter(Boolean).join('；') || '-'
}

const formatOneLocation = (label, locationName, distanceMeters) => {
  if (!locationName && (distanceMeters === null || distanceMeters === undefined)) return ''
  const distanceText = distanceMeters === null || distanceMeters === undefined ? '距离未记录' : `距办公点 ${distanceMeters} 米`
  return `${label}：${locationName || '办公点'}，${distanceText}`
}

const getStatusTagType = (status) => {
  switch (status) {
    case 'NORMAL': return 'success'
    case 'RECHECKED': return 'primary'
    case 'ON_LEAVE': return 'warning'
    case 'LATE': return 'danger'
    case 'EARLY_LEAVE': return 'warning'
    case 'LATE_AND_EARLY': return 'danger'
    case 'ABSENT': return 'danger'
    case 'MISSING_CARD': return 'warning'
    default: return 'info'
  }
}

const formatStatusText = (status, lateMins, earlyMins) => {
  switch (status) {
    case 'NORMAL': return '正常打卡'
    case 'RECHECKED': return '已补卡'
    case 'ON_LEAVE': return '休假中'
    case 'LATE': return `迟到 (${formatMinutes(lateMins)})`
    case 'EARLY_LEAVE': return `早退 (${formatMinutes(earlyMins)})`
    case 'LATE_AND_EARLY': return `迟到(${formatMinutes(lateMins)}) + 早退(${formatMinutes(earlyMins)})`
    case 'ABSENT': return `旷工`
    case 'MISSING_CARD': return '缺卡'
    default: return status || '正常打卡'
  }
}

const getDeptStatusTagType = (status) => {
  switch (status) {
    case 'NORMAL': return 'success'
    case 'RECHECKED': return 'primary'
    case 'ON_LEAVE': return 'warning'
    case 'LATE': return 'danger'
    case 'EARLY_LEAVE': return 'warning'
    case 'LATE_AND_EARLY': return 'danger'
    case 'ABSENT': return 'danger'
    case 'MISSING_CARD': return 'warning'
    case 'NOT_CHECKED': return 'info'
    default: return 'info'
  }
}

const formatDeptStatusText = (status, lateMins) => {
  switch (status) {
    case 'NORMAL': return '正常打卡'
    case 'RECHECKED': return '已补卡'
    case 'ON_LEAVE': return '休假中'
    case 'LATE': return `迟到 (${formatMinutes(lateMins)})`
    case 'EARLY_LEAVE': return '早退'
    case 'LATE_AND_EARLY': return `迟到且早退 (${formatMinutes(lateMins)})`
    case 'ABSENT': return `旷工`
    case 'MISSING_CARD': return '缺卡'
    case 'NOT_CHECKED': return '未打卡'
    default: return status || '未打卡'
  }
}

onMounted(() => {
  updateClock()
  timer = setInterval(updateClock, 1000)
  loadActiveTabData(activeTab.value)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.attendance-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 顶部概览 Banner (白色卡片) */
.overview-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.live-clock {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
  font-family: 'Inter', -apple-system, sans-serif;
  letter-spacing: -1px;
}

.date-info {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.date-text {
  font-size: 14px;
  color: #64748b;
}

.metrics-grid {
  display: flex;
  gap: 36px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.metric-title {
  font-size: 13px;
  color: #64748b;
}

.metric-num {
  font-size: 24px;
  font-weight: 700;
  margin-top: 4px;
}

.metric-num small {
  font-size: 13px;
  font-weight: 400;
  color: #94a3b8;
}

.primary-num { color: #2563eb; }
.success-num { color: #16a34a; }
.warning-num { color: #ea580c; }
.danger-num { color: #dc2626; }

/* 打卡卡片区 */
.punch-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px 24px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.punch-card:hover {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f8fafc;
}

.head-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.target-time {
  font-size: 12px;
  font-weight: 400;
  color: #94a3b8;
  margin-left: 6px;
}

.icon-sun { color: #ea580c; font-size: 20px; }
.icon-moon { color: #6366f1; font-size: 20px; }

.card-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 16px;
}

.record-detail {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.detail-label {
  font-size: 13px;
  color: #64748b;
}

.detail-time {
  font-size: 20px;
  font-weight: 700;
}

.detail-time small {
  font-size: 12px;
  font-weight: 400;
  color: #64748b;
}

.detail-hint {
  font-size: 13px;
  color: #94a3b8;
}

.action-button {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 8px;
}

.success-text { color: #16a34a; }
.warning-text { color: #d97706; }
.text-muted { color: #94a3b8; }

/* 底部表格卡片 */
.table-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.table-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title-group h2 {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.header-icon {
  font-size: 20px;
  color: #2563eb;
}

.header-filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.date-column {
  font-weight: 600;
  color: #334155;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.location-cell {
  color: #475569;
  font-size: 13px;
  line-height: 1.5;
}

.rule-location-cell {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  color: #475569;
  font-size: 13px;
}

.punch-location-panel {
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  background: #f8fafc;
}

.punch-location-panel.location-success {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.punch-location-panel.location-error {
  border-color: #fecaca;
  background: #fef2f2;
}

.punch-location-panel.location-loading {
  border-color: #fed7aa;
  background: #fff7ed;
}

.location-panel-main {
  display: flex;
  gap: 10px;
  min-width: 0;
}

.location-title {
  font-weight: 600;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 8px;
}

.location-desc {
  margin-top: 4px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.location-meta {
  margin-top: 4px;
  color: #475569;
  font-size: 12px;
}

.office-location-editor {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 10px;
}

.office-location-editor :deep(.el-input-number) {
  width: 100%;
}

.form-tip {
  width: 100%;
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 768px) {
  .punch-location-panel {
    flex-direction: column;
  }

  .office-location-editor {
    grid-template-columns: 1fr;
  }
}
</style>
