import { useState, useEffect, useRef } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import {
  PaperAirplaneIcon,
  ChatBubbleLeftRightIcon,
  ArrowLeftIcon,
  UserGroupIcon,
  UserIcon,
  ArrowRightOnRectangleIcon,
} from '@heroicons/react/24/outline';
import { messageService } from '../services/messageService';
import { groupService } from '../services/groupService';
import userService from '../services/userService';
import { Message, Conversation, User, IdeaGroup, GroupMessage } from '../types';
import { useAuth } from '../context/AuthContext';
import { format, isToday, isYesterday } from 'date-fns';
import toast from 'react-hot-toast';

type ChatMode = 'direct' | 'group';

export default function MessagesPage() {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // Modus-Status
  const [activeTab, setActiveTab] = useState<ChatMode>('direct');

  // Direktnachrichten-Status
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);

  // Gruppenchat-Status
  const [groups, setGroups] = useState<IdeaGroup[]>([]);
  const [groupMessages, setGroupMessages] = useState<GroupMessage[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<IdeaGroup | null>(null);

  // Gemeinsamer Status
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [sendingMessage, setSendingMessage] = useState(false);
  const [showMobileList, setShowMobileList] = useState(true);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [allUsers, setAllUsers] = useState<User[]>([]);
  const [showNewConversation, setShowNewConversation] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  // URL-Parameter für Benutzer oder Gruppe prüfen
  useEffect(() => {
    const userId = searchParams.get('user');
    const groupId = searchParams.get('group');

    if (groupId) {
      setActiveTab('group');
      loadGroupFromUrl(Number(groupId));
    } else if (userId) {
      setActiveTab('direct');
      loadConversationFromUrl(Number(userId));
    }
  }, [searchParams]);

  useEffect(() => {
    fetchConversations();
    fetchGroups();
    fetchAllUsers();
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, groupMessages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const loadConversationFromUrl = async (userId: number) => {
    try {
      const users = await userService.getAllUsers();
      // Number()-Konvertierung verwenden, um Typkonsistenz sicherzustellen
      const targetUser = users.find(u => Number(u.id) === Number(userId));
      if (targetUser) {
        setSelectedUser(targetUser);
        setSelectedGroup(null);
        setShowMobileList(false);
        setActiveTab('direct');
        await fetchMessages(targetUser.id);
      }
    } catch (error) {
      console.error('Failed to load conversation from URL:', error);
    }
  };

  const loadGroupFromUrl = async (groupId: number) => {
    try {
      const group = await groupService.getGroup(groupId);
      setSelectedGroup(group);
      setSelectedUser(null);
      setShowMobileList(false);
      await fetchGroupMessages(groupId);
    } catch (error) {
      console.error('Failed to load group from URL:', error);
    }
  };

  const fetchConversations = async () => {
    try {
      const data = await messageService.getConversations();
      setConversations(data);
    } catch (error) {
      console.error('Failed to fetch conversations:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchGroups = async () => {
    try {
      const data = await groupService.getUserGroups();
      setGroups(data);
    } catch (error) {
      console.error('Failed to fetch groups:', error);
    }
  };

  const fetchAllUsers = async () => {
    try {
      const users = await userService.getAllUsers();
      setAllUsers(users.filter(u => u.id !== user?.id));
    } catch (error) {
      console.error('Failed to fetch users:', error);
    }
  };

  const fetchMessages = async (userId: number) => {
    try {
      const data = await messageService.getConversation(userId);
      setMessages(data);
      await messageService.markConversationAsRead(userId);
      setConversations(prev =>
        prev.map(c =>
          c.otherUser.id === userId ? { ...c, unreadCount: 0 } : c
        )
      );
    } catch (error) {
      console.error('Failed to fetch messages:', error);
    }
  };

  const fetchGroupMessages = async (groupId: number) => {
    try {
      const data = await groupService.getGroupMessages(groupId);
      setGroupMessages(data);
      await groupService.markAllAsRead(groupId);
      setGroups(prev =>
        prev.map(g =>
          g.id === groupId ? { ...g, unreadCount: 0 } : g
        )
      );
    } catch (error) {
      console.error('Failed to fetch group messages:', error);
    }
  };

  const handleSelectConversation = async (otherUser: User) => {
    setSelectedUser(otherUser);
    setSelectedGroup(null);
    setShowMobileList(false);
    setShowNewConversation(false);
    setSearchParams({ user: otherUser.id.toString() });
    await fetchMessages(otherUser.id);
  };

  const handleSelectGroup = async (group: IdeaGroup) => {
    setSelectedGroup(group);
    setSelectedUser(null);
    setShowMobileList(false);
    setSearchParams({ group: group.id.toString() });
    await fetchGroupMessages(group.id);
  };

  const handleStartNewConversation = (targetUser: User) => {
    setSelectedUser(targetUser);
    setSelectedGroup(null);
    setMessages([]);
    setShowNewConversation(false);
    setShowMobileList(false);
    setSearchParams({ user: targetUser.id.toString() });
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMessage.trim()) return;

    setSendingMessage(true);
    try {
      if (selectedGroup) {
        // Gruppennachricht senden
        const message = await groupService.sendGroupMessage(selectedGroup.id, newMessage.trim());
        setGroupMessages(prev => [...prev, message]);
        setNewMessage('');

        // Letzte Nachricht der Gruppe aktualisieren
        setGroups(prev => {
          const updated = prev.map(g =>
            g.id === selectedGroup.id
              ? { ...g, lastMessage: message, updatedAt: message.createdAt }
              : g
          );
          return updated.sort((a, b) =>
            new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
          );
        });
      } else if (selectedUser) {
        // Direktnachricht senden
        const message = await messageService.sendMessage({
          recipientId: selectedUser.id,
          content: newMessage.trim(),
        });
        setMessages(prev => [...prev, message]);
        setNewMessage('');

        const existingConvIndex = conversations.findIndex(c => c.otherUser.id === selectedUser.id);
        if (existingConvIndex >= 0) {
          setConversations(prev => {
            const updated = [...prev];
            updated[existingConvIndex] = {
              ...updated[existingConvIndex],
              lastMessage: message,
              lastMessageAt: message.createdAt,
            };
            return updated.sort((a, b) =>
              new Date(b.lastMessageAt).getTime() - new Date(a.lastMessageAt).getTime()
            );
          });
        } else {
          setConversations(prev => [{
            otherUser: selectedUser,
            lastMessage: message,
            unreadCount: 0,
            lastMessageAt: message.createdAt,
          }, ...prev]);
        }
      }
    } catch (error) {
      toast.error('Fehler beim Senden der Nachricht');
    } finally {
      setSendingMessage(false);
    }
  };

  const handleLeaveGroup = async (groupId: number) => {
    if (!confirm('Sind Sie sicher, dass Sie diese Gruppe verlassen möchten?')) return;

    try {
      await groupService.leaveGroup(groupId);
      setGroups(prev => prev.filter(g => g.id !== groupId));
      setSelectedGroup(null);
      setSearchParams({});
      toast.success('Gruppe erfolgreich verlassen');
    } catch (error) {
      toast.error('Fehler beim Verlassen der Gruppe');
    }
  };

  const handleBackToList = () => {
    setShowMobileList(true);
    setSelectedUser(null);
    setSelectedGroup(null);
    setSearchParams({});
  };

  const formatMessageDate = (dateStr: string) => {
    const date = new Date(dateStr);
    if (isToday(date)) {
      return format(date, 'HH:mm');
    } else if (isYesterday(date)) {
      return 'Gestern ' + format(date, 'HH:mm');
    }
    return format(date, 'd. MMM, HH:mm');
  };

  const formatConversationDate = (dateStr: string) => {
    const date = new Date(dateStr);
    if (isToday(date)) {
      return format(date, 'HH:mm');
    } else if (isYesterday(date)) {
      return 'Gestern';
    }
    return format(date, 'd. MMM');
  };

  const filteredUsers = allUsers.filter(u =>
    u.firstName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    u.lastName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
    u.username?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-600" />
      </div>
    );
  }

  return (
    <div className="h-[calc(100vh-180px)] flex flex-col">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Nachrichten</h1>
        <button
          onClick={() => setShowNewConversation(true)}
          className="btn-primary"
        >
          Neue Nachricht
        </button>
      </div>

      <div className="flex-1 flex card overflow-hidden">
        {/* Konversations-/Gruppenliste */}
        <div className={`w-full md:w-80 border-r border-gray-200 dark:border-gray-700 flex flex-col ${
          !showMobileList ? 'hidden md:flex' : 'flex'
        }`}>
          {/* Reiter */}
          <div className="flex border-b border-gray-200 dark:border-gray-700">
            <button
              onClick={() => setActiveTab('direct')}
              className={`flex-1 p-3 flex items-center justify-center gap-2 font-medium transition-colors ${
                activeTab === 'direct'
                  ? 'text-primary-600 border-b-2 border-primary-600'
                  : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
              }`}
            >
              <UserIcon className="w-4 h-4" />
              Direkt
            </button>
            <button
              onClick={() => setActiveTab('group')}
              className={`flex-1 p-3 flex items-center justify-center gap-2 font-medium transition-colors ${
                activeTab === 'group'
                  ? 'text-primary-600 border-b-2 border-primary-600'
                  : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
              }`}
            >
              <UserGroupIcon className="w-4 h-4" />
              Gruppen
              {groups.reduce((sum, g) => sum + g.unreadCount, 0) > 0 && (
                <span className="bg-primary-600 text-white text-xs rounded-full px-1.5 py-0.5">
                  {groups.reduce((sum, g) => sum + g.unreadCount, 0)}
                </span>
              )}
            </button>
          </div>

          <div className="flex-1 overflow-y-auto">
            {activeTab === 'direct' ? (
              // Direktkonversationen
              conversations.length === 0 ? (
                <div className="p-8 text-center text-gray-500 dark:text-gray-400">
                  <ChatBubbleLeftRightIcon className="w-12 h-12 mx-auto mb-3 opacity-50" />
                  <p>Noch keine Konversationen</p>
                  <p className="text-sm mt-1">Starten Sie eine neue Nachricht, um zu chatten</p>
                </div>
              ) : (
                conversations.map((conv) => (
                  <button
                    key={conv.otherUser.id}
                    onClick={() => handleSelectConversation(conv.otherUser)}
                    className={`w-full p-4 flex items-start gap-3 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors text-left border-b border-gray-100 dark:border-gray-700 ${
                      selectedUser?.id === conv.otherUser.id
                        ? 'bg-primary-50 dark:bg-primary-900/20'
                        : ''
                    }`}
                  >
                    <div className="avatar-md flex-shrink-0">
                      {conv.otherUser.firstName?.[0]}{conv.otherUser.lastName?.[0]}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <span className="font-medium text-gray-900 dark:text-white truncate">
                          {conv.otherUser.firstName} {conv.otherUser.lastName}
                        </span>
                        {conv.lastMessageAt && (
                          <span className="text-xs text-gray-500 dark:text-gray-400 flex-shrink-0">
                            {formatConversationDate(conv.lastMessageAt)}
                          </span>
                        )}
                      </div>
                      <div className="flex items-center justify-between mt-1">
                        <p className="text-sm text-gray-500 dark:text-gray-400 truncate">
                          {conv.lastMessage?.content}
                        </p>
                        {conv.unreadCount > 0 && (
                          <span className="ml-2 bg-primary-600 text-white text-xs rounded-full px-2 py-0.5 flex-shrink-0">
                            {conv.unreadCount}
                          </span>
                        )}
                      </div>
                    </div>
                  </button>
                ))
              )
            ) : (
              // Gruppenchats
              groups.length === 0 ? (
                <div className="p-8 text-center text-gray-500 dark:text-gray-400">
                  <UserGroupIcon className="w-12 h-12 mx-auto mb-3 opacity-50" />
                  <p>Noch keine Gruppenchats</p>
                  <p className="text-sm mt-1">Treten Sie einer Ideengruppe bei, um zu chatten</p>
                  <Link to="/ideas" className="btn-primary mt-4 inline-block">
                    Ideen durchsuchen
                  </Link>
                </div>
              ) : (
                groups.map((group) => (
                  <button
                    key={group.id}
                    onClick={() => handleSelectGroup(group)}
                    className={`w-full p-4 flex items-start gap-3 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors text-left border-b border-gray-100 dark:border-gray-700 ${
                      selectedGroup?.id === group.id
                        ? 'bg-primary-50 dark:bg-primary-900/20'
                        : ''
                    }`}
                  >
                    <div className="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900/50 flex items-center justify-center flex-shrink-0">
                      <UserGroupIcon className="w-5 h-5 text-primary-600 dark:text-primary-400" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <span className="font-medium text-gray-900 dark:text-white truncate">
                          {group.name}
                        </span>
                        {group.lastMessage && (
                          <span className="text-xs text-gray-500 dark:text-gray-400 flex-shrink-0">
                            {formatConversationDate(group.lastMessage.createdAt)}
                          </span>
                        )}
                      </div>
                      <div className="flex items-center justify-between mt-1">
                        <p className="text-sm text-gray-500 dark:text-gray-400 truncate">
                          {group.lastMessage
                            ? `${group.lastMessage.sender.firstName}: ${group.lastMessage.content}`
                            : `${group.memberCount} members`
                          }
                        </p>
                        {group.unreadCount > 0 && (
                          <span className="ml-2 bg-primary-600 text-white text-xs rounded-full px-2 py-0.5 flex-shrink-0">
                            {group.unreadCount}
                          </span>
                        )}
                      </div>
                    </div>
                  </button>
                ))
              )
            )}
          </div>
        </div>

        {/* Chat-Fenster */}
        <div className={`flex-1 flex flex-col ${
          showMobileList ? 'hidden md:flex' : 'flex'
        }`}>
          {selectedUser || selectedGroup ? (
            <>
              {/* Chat-Kopfzeile */}
              <div className="p-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <button
                    onClick={handleBackToList}
                    className="md:hidden btn-icon"
                  >
                    <ArrowLeftIcon className="w-5 h-5" />
                  </button>
                  {selectedGroup ? (
                    <>
                      <div className="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900/50 flex items-center justify-center">
                        <UserGroupIcon className="w-5 h-5 text-primary-600 dark:text-primary-400" />
                      </div>
                      <div>
                        <h3 className="font-medium text-gray-900 dark:text-white">
                          {selectedGroup.name}
                        </h3>
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                          {selectedGroup.memberCount} Mitglieder
                        </p>
                      </div>
                    </>
                  ) : selectedUser ? (
                    <>
                      <div className="avatar-md">
                        {selectedUser.firstName?.[0]}{selectedUser.lastName?.[0]}
                      </div>
                      <div>
                        <h3 className="font-medium text-gray-900 dark:text-white">
                          {selectedUser.firstName} {selectedUser.lastName}
                        </h3>
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                          @{selectedUser.username}
                        </p>
                      </div>
                    </>
                  ) : null}
                </div>
                {selectedGroup && selectedGroup.createdBy.id !== user?.id && (
                  <button
                    onClick={() => handleLeaveGroup(selectedGroup.id)}
                    className="btn-icon text-gray-400 hover:text-error-500"
                    title="Gruppe verlassen"
                  >
                    <ArrowRightOnRectangleIcon className="w-5 h-5" />
                  </button>
                )}
              </div>

              {/* Nachrichten */}
              <div className="flex-1 overflow-y-auto p-4 space-y-4">
                {selectedGroup ? (
                  // Gruppennachrichten
                  groupMessages.length === 0 ? (
                    <div className="h-full flex items-center justify-center text-gray-500 dark:text-gray-400">
                      <div className="text-center">
                        <UserGroupIcon className="w-12 h-12 mx-auto mb-3 opacity-50" />
                        <p>Noch keine Nachrichten</p>
                        <p className="text-sm">Seien Sie der Erste, der etwas sagt!</p>
                      </div>
                    </div>
                  ) : (
                    groupMessages.map((msg) => {
                      const isSentByMe = msg.sender.id === user?.id;
                      return (
                        <div
                          key={msg.id}
                          className={`flex ${isSentByMe ? 'justify-end' : 'justify-start'}`}
                        >
                          <div className={`flex gap-2 max-w-[70%] ${isSentByMe ? 'flex-row-reverse' : ''}`}>
                            {!isSentByMe && (
                              <div className="avatar-sm flex-shrink-0 mt-1">
                                {msg.sender.firstName?.[0]}{msg.sender.lastName?.[0]}
                              </div>
                            )}
                            <div>
                              {!isSentByMe && (
                                <p className="text-xs text-gray-500 dark:text-gray-400 mb-1 ml-1">
                                  {msg.sender.firstName} {msg.sender.lastName}
                                </p>
                              )}
                              <div
                                className={`rounded-2xl px-4 py-2 ${
                                  isSentByMe
                                    ? 'bg-primary-600 text-white rounded-br-sm'
                                    : 'bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-white rounded-bl-sm'
                                }`}
                              >
                                <p className="whitespace-pre-wrap break-words">{msg.content}</p>
                                <p className={`text-xs mt-1 ${
                                  isSentByMe ? 'text-primary-200' : 'text-gray-500 dark:text-gray-400'
                                }`}>
                                  {formatMessageDate(msg.createdAt)}
                                </p>
                              </div>
                            </div>
                          </div>
                        </div>
                      );
                    })
                  )
                ) : (
                  // Direktnachrichten
                  messages.length === 0 ? (
                    <div className="h-full flex items-center justify-center text-gray-500 dark:text-gray-400">
                      <div className="text-center">
                        <ChatBubbleLeftRightIcon className="w-12 h-12 mx-auto mb-3 opacity-50" />
                        <p>Noch keine Nachrichten</p>
                        <p className="text-sm">Senden Sie eine Nachricht, um die Konversation zu starten</p>
                      </div>
                    </div>
                  ) : (
                    messages.map((msg) => {
                      const isSentByMe = msg.sender.id === user?.id;
                      return (
                        <div
                          key={msg.id}
                          className={`flex ${isSentByMe ? 'justify-end' : 'justify-start'}`}
                        >
                          <div
                            className={`max-w-[70%] rounded-2xl px-4 py-2 ${
                              isSentByMe
                                ? 'bg-primary-600 text-white rounded-br-sm'
                                : 'bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-white rounded-bl-sm'
                            }`}
                          >
                            <p className="whitespace-pre-wrap break-words">{msg.content}</p>
                            {msg.ideaTitle && (
                              <p className={`text-xs mt-1 ${
                                isSentByMe ? 'text-primary-200' : 'text-gray-500 dark:text-gray-400'
                              }`}>
                                Re: {msg.ideaTitle}
                              </p>
                            )}
                            <p className={`text-xs mt-1 ${
                              isSentByMe ? 'text-primary-200' : 'text-gray-500 dark:text-gray-400'
                            }`}>
                              {formatMessageDate(msg.createdAt)}
                            </p>
                          </div>
                        </div>
                      );
                    })
                  )
                )}
                <div ref={messagesEndRef} />
              </div>

              {/* Nachrichteneingabe */}
              <form onSubmit={handleSendMessage} className="p-4 border-t border-gray-200 dark:border-gray-700">
                <div className="flex gap-3">
                  <input
                    type="text"
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    placeholder="Nachricht eingeben..."
                    maxLength={2000}
                    className="input flex-1"
                    autoFocus
                  />
                  <button
                    type="submit"
                    disabled={!newMessage.trim() || sendingMessage}
                    className="btn-primary px-4"
                  >
                    <PaperAirplaneIcon className="w-5 h-5" />
                  </button>
                </div>
              </form>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-gray-500 dark:text-gray-400">
              <div className="text-center">
                <ChatBubbleLeftRightIcon className="w-16 h-16 mx-auto mb-4 opacity-50" />
                <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
                  Wählen Sie eine Konversation
                </h3>
                <p>Wählen Sie eine Konversation aus der Liste oder starten Sie eine neue Nachricht</p>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Neue Konversation Modal */}
      {showNewConversation && (
        <div className="modal-overlay" onClick={() => setShowNewConversation(false)}>
          <div className="modal-content p-6 max-w-md" onClick={(e) => e.stopPropagation()}>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">
              Neue Nachricht
            </h2>

            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Benutzer suchen..."
              className="input mb-4"
              autoFocus
            />

            <div className="max-h-80 overflow-y-auto">
              {filteredUsers.length === 0 ? (
                <p className="text-center text-gray-500 dark:text-gray-400 py-4">
                  Keine Benutzer gefunden
                </p>
              ) : (
                filteredUsers.map((targetUser) => (
                  <button
                    key={targetUser.id}
                    onClick={() => handleStartNewConversation(targetUser)}
                    className="w-full p-3 flex items-center gap-3 hover:bg-gray-50 dark:hover:bg-gray-700/50 rounded-lg transition-colors"
                  >
                    <div className="avatar-md">
                      {targetUser.firstName?.[0]}{targetUser.lastName?.[0]}
                    </div>
                    <div className="text-left">
                      <p className="font-medium text-gray-900 dark:text-white">
                        {targetUser.firstName} {targetUser.lastName}
                      </p>
                      <p className="text-sm text-gray-500 dark:text-gray-400">
                        @{targetUser.username}
                      </p>
                    </div>
                  </button>
                ))
              )}
            </div>

            <div className="flex justify-end mt-4">
              <button
                onClick={() => setShowNewConversation(false)}
                className="btn-secondary"
              >
                Abbrechen
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
