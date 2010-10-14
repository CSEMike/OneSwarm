package edu.washington.cs.oneswarm.ui.gwt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.prefs.Preferences;

/**
 * Wrapper class for performing common windows registry functions
 * released under GPL, see 
 * http://code.google.com/p/javaregistrywrapper/
 * @author Vimal
 * 
 */
public class RegUtil
{

	/**
	 * Windows handles to HKEY_CURRENT_USER hive.
	 */
	public static final int					HKEY_CURRENT_USER			 = 0x80000001;

	/**
	 * Windows handles to HKEY_LOCAL_MACHINE hive.
	 */
	public static final int					HKEY_LOCAL_MACHINE			= 0x80000002;

	/* Windows error codes. */
	/**
	 * Registry Operation Successful
	 */
	public static final int					ERROR_SUCCESS					 = 0;

	/**
	 * Error because the specified Registry Key was not found
	 */
	public static final int					ERROR_FILE_NOT_FOUND		= 2;

	/**
	 * Error because acces to the specified key was denied
	 */
	public static final int					ERROR_ACCESS_DENIED		 = 5;

	/* Constants used to interpret returns of native functions */

	/**
	 * The index of Native Registry Handle in the return from opening/creating a
	 * key
	 */
	public static final int					NATIVE_HANDLE					 = 0;

	/**
	 * The index of Error Code in the return value
	 */
	public static final int					ERROR_CODE							= 1;

	/**
	 * Index pointing to the count of sub keys
	 */
	public static final int					SUBKEYS_NUMBER					= 0;

	/**
	 * Index pointing to the count of sub values
	 */
	public static final int					VALUES_NUMBER					 = 2;

	/**
	 * Index pointing to the max length of sub key
	 */
	public static final int					MAX_KEY_LENGTH					= 3;

	/**
	 * Index pointing to the max length of a value name
	 */
	public static final int					MAX_VALUE_NAME_LENGTH	 = 4;

	/**
	 * Index specifying whether new key was created or existing key was opened
	 */
	public static final int					DISPOSITION						 = 2;

	/**
	 * Value specifying that new key was created
	 */
	public static final int					REG_CREATED_NEW_KEY		 = 1;

	/**
	 * Value specifying that existing key was opened
	 */
	public static final int					REG_OPENED_EXISTING_KEY = 2;

	/**
	 * The null Native Handle
	 */
	public static final int					NULL_NATIVE_HANDLE			= 0;

	/**
	 * Security Masks
	 */

	/**
	 * Mask allowing permission to Delete
	 */
	public static final int					DELETE									= 0x10000;

	/**
	 * Mask allowing permission to Query
	 */
	public static final int					KEY_QUERY_VALUE				 = 1;

	/**
	 * Mask allowing permission to Set Value
	 */
	public static final int					KEY_SET_VALUE					 = 2;

	/**
	 * Mask allowing permission to Create a Sub Key
	 */
	public static final int					KEY_CREATE_SUB_KEY			= 4;

	/**
	 * Mask allowing permission to enumerate sub keys
	 */
	public static final int					KEY_ENUMERATE_SUB_KEYS	= 8;

	/**
	 * Mask allowing permission to read a value
	 */
	public static final int					KEY_READ								= 0x20019;

	/**
	 * Mask allowing permission to write/create a value
	 */
	public static final int					KEY_WRITE							 = 0x20006;

	/**
	 * Mask allowing all access permission
	 */
	public static final int					KEY_ALL_ACCESS					= 0xf003f;

	private static final Preferences userRoot								= Preferences.userRoot();

	private static final Preferences systemRoot							= Preferences.systemRoot();

	private static Class						 userClass							 = null;

	private static Class						 systemClass						 = null;

	private static Method						windowsRegOpenKey			 = null;

	private static Method						windowsRegCloseKey			= null;

	private static Method						windowsRegCreateKeyEx	 = null;

	private static Method						windowsRegDeleteKey		 = null;

	private static Method						windowsRegFlushKey			= null;

	private static Method						windowsRegQueryValueEx	= null;

	private static Method						windowsRegSetValueEx		= null;

	private static Method						windowsRegDeleteValue	 = null;

	private static Method						windowsRegQueryInfoKey	= null;

	private static Method						windowsRegEnumKeyEx		 = null;

	private static Method						windowsRegEnumValue		 = null;

	static {
		userClass = userRoot.getClass();
		systemClass = systemRoot.getClass();
		try {
			windowsRegOpenKey = userClass.getDeclaredMethod("WindowsRegOpenKey",
					new Class[] {
		int.class,
		byte[].class,
		int.class
					});
			windowsRegOpenKey.setAccessible(true);

			windowsRegCloseKey = userClass.getDeclaredMethod("WindowsRegCloseKey",
					new Class[] {
						int.class
					});
			windowsRegCloseKey.setAccessible(true);

			windowsRegCreateKeyEx = userClass.getDeclaredMethod(
					"WindowsRegCreateKeyEx", new Class[] {
		int.class,
		byte[].class
					});
			windowsRegCreateKeyEx.setAccessible(true);

			windowsRegDeleteKey = userClass.getDeclaredMethod("WindowsRegDeleteKey",
					new Class[] {
		int.class,
		byte[].class
					});
			windowsRegDeleteKey.setAccessible(true);

			windowsRegFlushKey = userClass.getDeclaredMethod("WindowsRegFlushKey",
					new Class[] {
						int.class
					});
			windowsRegFlushKey.setAccessible(true);

			windowsRegQueryValueEx = userClass.getDeclaredMethod(
					"WindowsRegQueryValueEx", new Class[] {
		int.class,
		byte[].class
					});
			windowsRegQueryValueEx.setAccessible(true);

			windowsRegSetValueEx = userClass.getDeclaredMethod(
					"WindowsRegSetValueEx", new Class[] {
		int.class,
		byte[].class,
		byte[].class
					});
			windowsRegSetValueEx.setAccessible(true);

			windowsRegDeleteValue = userClass.getDeclaredMethod(
					"WindowsRegDeleteValue", new Class[] {
		int.class,
		byte[].class
					});
			windowsRegDeleteValue.setAccessible(true);

			windowsRegQueryInfoKey = userClass.getDeclaredMethod(
					"WindowsRegQueryInfoKey", new Class[] {
						int.class
					});
			windowsRegQueryInfoKey.setAccessible(true);

			windowsRegEnumKeyEx = userClass.getDeclaredMethod("WindowsRegEnumKeyEx",
					new Class[] {
		int.class,
		int.class,
		int.class
					});
			windowsRegEnumKeyEx.setAccessible(true);

			windowsRegEnumValue = userClass.getDeclaredMethod("WindowsRegEnumValue",
					new Class[] {
		int.class,
		int.class,
		int.class
					});
			windowsRegEnumValue.setAccessible(true);

		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Java wrapper for Windows registry API RegOpenKey()

	 * 
	 * @param hKey
	 *            Handle to the root key
	 * @param subKey
	 *            The sub key as a byte array
	 * @param securityMask
	 *            The security Mask
	 * @return int[2] An Integer Array retval[NATIVE_HANDLE] will be the Native
	 *         Handle of the registry entry retval[ERROR_CODE] will be the error
	 *         code ERROR_SUCCESS means success
	 */
	public static int[] RegOpenKey(int hKey, byte[] subKey, int securityMask) {
		try {
			return (int[]) windowsRegOpenKey.invoke(systemRoot, new Object[] {
				new Integer(hKey),
				subKey,
				new Integer(securityMask)
			});
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new int[0];
	}

	/**
	 * 
	 * Java wrapper for Windows registry APIRegOpenKey()

	 * 
	 * @param hKey
	 *            Handle to the root key
	 * @param subKey
	 *            The sub key
	 * @param securityMask
	 *            The security Mask
	 * @return int[2] An Integer Array retval[NATIVE_HANDLE] will be the Native
	 *         Handle of the registry entry retval[ERROR_CODE] will be the error
	 *         code ERROR_SUCCESS means success
	 */
	public static int[] RegOpenKey(int hKey, String subKey, int securityMask) {
		return RegOpenKey(hKey, stringToByteArray(subKey), securityMask);
	}

	/**
	 * Java wrapper for Windows registry API RegCloseKey()
	 * 
	 * @param hKey
	 *            The Native Handle
	 * @return The Error Code
	 */
	public static int RegCloseKey(int hKey) {
		try {
			return ((Integer) windowsRegCloseKey.invoke(systemRoot, new Object[] {
				new Integer(hKey)
			})).intValue();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ERROR_ACCESS_DENIED;
	}

	/**
	 * Java wrapper for Windows registry API RegCreateKeyEx()
	 * 
	 * @param hKey
	 *            The Native Hanle to the Key
	 * @param subKey
	 *            The Sub key as a byte array
	 * @return Int[3] etval[NATIVE_HANDLE] will be the Native Handle of the
	 *         registry entry, retval[ERROR_CODE] will be the error code
	 *         ERROR_SUCCESS means success retval[DISPOSITION] will indicate
	 *         whether key was created or existing key was opened
	 */
	public static int[] RegCreateKeyEx(int hKey, byte[] subKey) {
		try {
			return (int[]) windowsRegCreateKeyEx.invoke(systemRoot, new Object[] {
				new Integer(hKey),
				subKey
			});
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new int[0];
	}

	/**
	 * Java wrapper for Windows registry API RegCreateKeyEx()
	 * 
	 * @param hKey
	 *            The Native Hanle to the Key
	 * @param subKey
	 *            The Sub key
	 * @return Int[3] etval[NATIVE_HANDLE] will be the Native Handle of the
	 *         registry entry, retval[ERROR_CODE] will be the error code
	 *         ERROR_SUCCESS means success retval[DISPOSITION] will indicate
	 *         whether key was created or existing key was opened
	 */
	public static int[] RegCreateKeyEx(int hKey, String subKey) {
		return RegCreateKeyEx(hKey, stringToByteArray(subKey));
	}

	/**
	 * Java wrapper for Windows registry API RegDeleteKey()
	 * 
	 * @param hKey
	 *            The Native Handle to a Key
	 * @param subKey
	 *            The sub key to be deleted as a byte array
	 * @return The Error Code
	 */
	public static int RegDeleteKey(int hKey, byte[] subKey) {
		try {
			return ((Integer) windowsRegDeleteKey.invoke(systemRoot, new Object[] {
				new Integer(hKey),
				subKey
			})).intValue();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ERROR_ACCESS_DENIED;
	}

	/**
	 * Java wrapper for Windows registry API RegDeleteKey()
	 * 
	 * @param hKey
	 *            The Native Handle to a Key
	 * @param subKey
	 *            The sub key to be deleted
	 * @return The Error Code
	 */
	public static int RegDeleteKey(int hKey, String subKey) {
		return RegDeleteKey(hKey, stringToByteArray(subKey));

	}

	/**
	 * Java wrapper for Windows registry API RegFlushKey()
	 * 
	 * @param hKey
	 *            The native handle
	 * @return the error code
	 */
	public static int RegFlushKey(int hKey) {
		try {
			return ((Integer) windowsRegFlushKey.invoke(systemRoot, new Object[] {
				new Integer(hKey)
			})).intValue();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ERROR_ACCESS_DENIED;
	}

	/**
	 * Java wrapper for Windows registry API RegQueryValueEx()
	 * 
	 * @param hKey
	 *            The Native Handle
	 * @param valueName
	 *            The Name of value to be queried as a byte array
	 * @return The value queried
	 */
	public static byte[] RegQueryValueEx(int hKey, byte[] valueName) {
		try {
			return (byte[]) windowsRegQueryValueEx.invoke(systemRoot, new Object[] {
				new Integer(hKey),
				valueName
			});
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new byte[0];
	}

	/**
	 * Java wrapper for Windows registry API RegQueryValueEx()
	 * 
	 * @param hKey
	 *            The Native Handle
	 * @param valueName
	 *            The Name of value to be queried
	 * @return The value queried
	 */
	public static byte[] RegQueryValueEx(int hKey, String valueName) {
		return RegQueryValueEx(hKey, stringToByteArray(valueName));
	}

	/**
	 * Java wrapper for Windows registry API RegSetValueEx()
	 * Creates a value if it didnt exist or will overwrite existing value
	 * 
	 * @param hKey
	 *            the Native Handle to the key
	 * @param valueName
	 *            The name of the value as a byet array
	 * @param value
	 *            The new vaue to be set as a byte array
	 * @return The error code
	 */
	public static int RegSetValueEx(int hKey, byte[] valueName, byte[] value) {
		try {
			return ((Integer) windowsRegSetValueEx.invoke(systemRoot, new Object[] {
				new Integer(hKey),
				valueName,
				value
			})).intValue();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ERROR_ACCESS_DENIED;
	}

	/**
	 * Java wrapper for Windows registry API RegSetValueEx()
	 * Creates a value if it didnt exist or will overwrite existing value
	 * 
	 * @param hKey
	 *            the Native Handle to the key
	 * @param valueName
	 *            The name of the value as a byet array
	 * @param value
	 *            The new vaue to be set as a byte array
	 * @return The error code
	 */
	public static int RegSetValueEx(int hKey, String valueName, String value) {
		return RegSetValueEx(hKey, stringToByteArray(valueName),
				stringToByteArray(value));
	}

	/**
	 * Java wrapper for Windows registry API RegDeleteValue()
	 * 
	 * @param hKey
	 *            The native Handle
	 * @param valueName
	 *            The sub key name as a byte array
	 * @return The error code
	 */
	public static int RegDeleteValue(int hKey, byte[] valueName) {
		try {
			return ((Integer) windowsRegDeleteValue.invoke(systemRoot, new Object[] {
				new Integer(hKey),
				valueName
			})).intValue();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ERROR_ACCESS_DENIED;
	}

	/**
	 * Java wrapper for Windows registry API RegDeleteValue()
	 * 
	 * @param hKey
	 *            The native Handle
	 * @param valueName
	 *            The sub key name
	 * @return The error code
	 */
	public static int RegDeleteValue(int hKey, String valueName) {
		return RegDeleteValue(hKey, stringToByteArray(valueName));
	}

	/**
	 * Java wrapper for Windows registry API RegQueryInfoKey()
	 * 
	 * @param hKey
	 *            the Native Handle
	 * @return int[5] retval[SUBKEYS_NUMBER] will give then number of sub keys
	 *         under the queried key. retval[ERROR_CODE] will give the error
	 *         code. retval[VALUES_NUMBER] will give the number of values under
	 *         the given key. retval[MAX_KEY_LENGTH] will give the length of the
	 *         longes sub key. retval[MAX_VALUE_NAME_LENGTH] will give length of
	 *         the longes value name.
	 */
	public static int[] RegQueryInfoKey(int hKey) {
		try {
			return (int[]) windowsRegQueryInfoKey.invoke(systemRoot, new Object[] {
				new Integer(hKey)
			});
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new int[0];
	}

	/**
	 * Java wrapper for Windows registry API RegEnumKeyEx()
	 * @param hKey The native Handle
	 * @param subKeyIndex Index of sub key
	 * @param maxKeyLength The length of max sub key
	 * @return The name of the sub key
	 */
	public static byte[] RegEnumKeyEx(int hKey, int subKeyIndex, int maxKeyLength) {
		try {
			return (byte[]) windowsRegEnumKeyEx.invoke(systemRoot, new Object[] {
				new Integer(hKey),
				new Integer(subKeyIndex),
				new Integer(maxKeyLength)
			});
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new byte[0];
	}

	/**
	 * Java wrapper for Windows registry API RegEnumValue()
	 * @param hKey The handle
	 * @param valueIndex the index of the value in the Key
	 * @param maxValueNameLength The max length of name
	 * @return The value
	 */
	public static byte[] RegEnumValue(int hKey, int valueIndex,
			int maxValueNameLength) {
		try {
			return (byte[]) windowsRegEnumValue.invoke(systemRoot, new Object[] {
				new Integer(hKey),
				new Integer(valueIndex),
				new Integer(maxValueNameLength)
			});
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new byte[0];
	}

	/**
	 * Returns this java string as a null-terminated byte array
	 */
	public static byte[] stringToByteArray(String str) {
		byte[] result = new byte[str.length() + 1];
		for (int i = 0; i < str.length(); i++) {
			result[i] = (byte) str.charAt(i);
		}
		result[str.length()] = 0;
		return result;
	}

}
