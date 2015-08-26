/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.engine.impl.persistence.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.Picture;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.UserQueryImpl;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.DbSqlSession;
import org.activiti.engine.impl.db.PersistentObject;
import org.activiti.engine.impl.interceptor.CommandContext;

/**
 * @author Tom Baeyens
 * @author Saeid Mirzaei
 * @author Joram Barrez
 */
public class UserEntityManagerImpl extends AbstractEntityManager<UserEntity> implements UserEntityManager {

  public User createNewUser(String userId) {
    return new UserEntity(userId);
  }

  public void insertUser(User user) {
    getDbSqlSession().insert((PersistentObject) user);

    if (getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
      getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_CREATED, user));
      getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_INITIALIZED, user));
    }
  }

  public void updateUser(User updatedUser) {
    CommandContext commandContext = Context.getCommandContext();
    DbSqlSession dbSqlSession = commandContext.getDbSqlSession();
    dbSqlSession.update((PersistentObject) updatedUser);

    if (getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
      getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_UPDATED, updatedUser));
    }
  }

  public User findUserById(String userId) {
    return (UserEntity) getDbSqlSession().selectOne("selectUserById", userId);
  }
  
  public void delete(UserEntity userEntity) {
    super.delete(userEntity);
    deletePicture(userEntity);
  }
  
  @Override
  public void deletePicture(User user) {
    UserEntity userEntity = (UserEntity) user;
    if (userEntity.getPictureByteArrayRef() != null) {
      userEntity.getPictureByteArrayRef().delete();
    }
  }

  @SuppressWarnings("unchecked")
  public void deleteUser(String userId) {
    UserEntity user = (UserEntity) findUserById(userId);
    if (user != null) {
      List<IdentityInfoEntity> identityInfos = getDbSqlSession().selectList("selectIdentityInfoByUserId", userId);
      for (IdentityInfoEntity identityInfo : identityInfos) {
        getIdentityInfoManager().delete(identityInfo);
      }
      getDbSqlSession().delete("deleteMembershipsByUserId", userId);

      delete(user);
    }
  }

  @SuppressWarnings("unchecked")
  public List<User> findUserByQueryCriteria(UserQueryImpl query, Page page) {
    return getDbSqlSession().selectList("selectUserByQueryCriteria", query, page);
  }

  public long findUserCountByQueryCriteria(UserQueryImpl query) {
    return (Long) getDbSqlSession().selectOne("selectUserCountByQueryCriteria", query);
  }

  @SuppressWarnings("unchecked")
  public List<Group> findGroupsByUser(String userId) {
    return getDbSqlSession().selectList("selectGroupsByUserId", userId);
  }

  public UserQuery createNewUserQuery() {
    return new UserQueryImpl(Context.getProcessEngineConfiguration().getCommandExecutor());
  }

  public IdentityInfoEntity findUserInfoByUserIdAndKey(String userId, String key) {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("userId", userId);
    parameters.put("key", key);
    return (IdentityInfoEntity) getDbSqlSession().selectOne("selectIdentityInfoByUserIdAndKey", parameters);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public List<String> findUserInfoKeysByUserIdAndType(String userId, String type) {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("userId", userId);
    parameters.put("type", type);
    return (List) getDbSqlSession().getSqlSession().selectList("selectIdentityInfoKeysByUserIdAndType", parameters);
  }

  public Boolean checkPassword(String userId, String password) {
    User user = findUserById(userId);
    if ((user != null) && (password != null) && (password.equals(user.getPassword()))) {
      return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  public List<User> findPotentialStarterUsers(String proceDefId) {
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put("procDefId", proceDefId);
    return (List<User>) getDbSqlSession().selectOne("selectUserByQueryCriteria", parameters);

  }

  @SuppressWarnings("unchecked")
  public List<User> findUsersByNativeQuery(Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return getDbSqlSession().selectListWithRawParameter("selectUserByNativeQuery", parameterMap, firstResult, maxResults);
  }

  public long findUserCountByNativeQuery(Map<String, Object> parameterMap) {
    return (Long) getDbSqlSession().selectOne("selectUserCountByNativeQuery", parameterMap);
  }

  @Override
  public boolean isNewUser(User user) {
    return ((UserEntity) user).getRevision() == 0;
  }

  @Override
  public Picture getUserPicture(String userId) {
    UserEntity user = (UserEntity) findUserById(userId);
    return user.getPicture();
  }

  @Override
  public void setUserPicture(String userId, Picture picture) {
    UserEntity user = (UserEntity) findUserById(userId);
    if (user == null) {
      throw new ActivitiObjectNotFoundException("user " + userId + " doesn't exist", User.class);
    }

    user.setPicture(picture);
  }

}
